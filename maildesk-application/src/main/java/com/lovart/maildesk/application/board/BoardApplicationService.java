package com.lovart.maildesk.application.board;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.BoardFunnelStageDto;
import com.lovart.maildesk.application.dto.BoardKolDto;
import com.lovart.maildesk.application.dto.BoardKolLatestEmailDto;
import com.lovart.maildesk.application.dto.BoardKpiDto;
import com.lovart.maildesk.application.dto.BoardMemberRowDto;
import com.lovart.maildesk.application.dto.BoardPlatformSegmentDto;
import com.lovart.maildesk.application.dto.BoardStageDistributionDto;
import com.lovart.maildesk.application.dto.BoardSummaryDto;
import com.lovart.maildesk.application.dto.PageMetaDto;
import com.lovart.maildesk.application.support.BoardDetailFilter;
import com.lovart.maildesk.application.support.BoardOwnerScope;
import com.lovart.maildesk.application.support.BoardWindow;
import com.lovart.maildesk.application.support.StageCatalog;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.enums.Platform;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.time.format.DateTimeFormatter;

@Service
public class BoardApplicationService {

    private static final int EMAIL_FETCH_LIMIT = 5000;
    private static final int RECENT_ACTIVITY_LIMIT = 16;
    private static final int DEFAULT_KOL_PAGE_SIZE = 20;
    private static final int MAX_KOL_PAGE_SIZE = 100;

    private static final List<Platform> PLATFORM_ORDER = List.of(
            Platform.TIKTOK,
            Platform.INSTAGRAM,
            Platform.YOUTUBE,
            Platform.X,
            Platform.OTHER
    );

    private static final Map<Platform, String> PLATFORM_LABELS = Map.of(
            Platform.TIKTOK, "TikTok",
            Platform.INSTAGRAM, "Instagram",
            Platform.YOUTUBE, "YouTube",
            Platform.X, "X",
            Platform.OTHER, "其他"
    );

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final List<KolStage> COOPERATION_STAGES = List.of(
            KolStage.CONFIRMED,
            KolStage.PRODUCING,
            KolStage.REVIEWING,
            KolStage.PUBLISHED,
            KolStage.PAYING
    );

    private final KolMapper kols;
    private final EmailMapper emails;
    private final ProfileMapper profiles;

    public BoardApplicationService(KolMapper kols, EmailMapper emails, ProfileMapper profiles) {
        this.kols = kols;
        this.emails = emails;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public BoardSummaryDto getBoard(
            String windowParam,
            UUID ownerId,
            boolean includeInterns,
            String detailParam,
            String stageParam,
            Integer pageParam,
            Integer sizeParam
    ) {
        BoardWindow window = BoardWindow.parse(windowParam);
        List<ProfileDO> activeMembers = BoardOwnerScope.activeBoardMembers(
                profiles.selectList(new LambdaQueryWrapper<ProfileDO>()));
        Set<UUID> scopeOwnerIds = BoardOwnerScope.resolveScopeOwnerIds(ownerId, includeInterns, activeMembers);
        Set<UUID> internOwnerIds = BoardOwnerScope.internMemberIds(activeMembers);

        List<KolDO> rows = kols.selectList(new LambdaQueryWrapper<KolDO>());
        List<KolDO> windowed = rows.stream()
                .filter(k -> window.matches(k.getFeishuOutreachAt()))
                .toList();
        List<KolDO> scoped = windowed.stream()
                .filter(k -> BoardOwnerScope.matchesBoardKolScope(k, scopeOwnerIds, includeInterns, internOwnerIds))
                .toList();

        Map<KolStage, Integer> snapshot = new EnumMap<>(KolStage.class);
        for (KolStage stage : StageCatalog.ALL_STAGES) {
            snapshot.put(stage, 0);
        }
        for (KolDO kol : scoped) {
            if (kol.getStage() != null) {
                snapshot.merge(kol.getStage(), 1, Integer::sum);
            }
        }

        int total = scoped.size();
        int unreplied = (int) scoped.stream().filter(BoardApplicationService::needsReply).count();
        int unreadEmails = countUnreadInboundEmails(scoped.stream().map(KolDO::getId).toList());
        int cooperation = COOPERATION_STAGES.stream()
                .mapToInt(stage -> snapshot.getOrDefault(stage, 0))
                .sum();

        int outreachCum = StageCatalog.cumulativeCount(snapshot, KolStage.OUTREACH);
        int payingCum = StageCatalog.cumulativeCount(snapshot, KolStage.PAYING);
        float conversion = outreachCum > 0 ? (float) payingCum / outreachCum : 0f;

        BoardKpiDto kpi = new BoardKpiDto(total, unreplied, unreadEmails, cooperation, conversion);

        List<BoardFunnelStageDto> funnel = StageCatalog.FUNNEL_STAGES.stream()
                .map(stage -> new BoardFunnelStageDto(
                        StageCatalog.jsonStage(stage),
                        StageCatalog.label(stage),
                        StageCatalog.cumulativeCount(snapshot, stage)
                ))
                .toList();

        List<BoardStageDistributionDto> distribution = StageCatalog.ALL_STAGES.stream()
                .map(stage -> new BoardStageDistributionDto(
                        StageCatalog.jsonStage(stage),
                        StageCatalog.label(stage),
                        snapshot.getOrDefault(stage, 0)
                ))
                .toList();

        List<BoardKolDto> kolRows;
        PageMetaDto kolsPage;
        List<BoardKolDto> recentActivity;
        List<BoardPlatformSegmentDto> platformDistribution;
        if (scoped.isEmpty()) {
            KolPageResult kolPage = buildKolPage(scoped, detailParam, stageParam, Map.of(), pageParam, sizeParam);
            kolRows = kolPage.rows();
            kolsPage = kolPage.pageMeta();
            recentActivity = List.of();
            platformDistribution = List.of();
        } else {
            Map<UUID, KolEmailSnapshot> scopedSnapshots = loadEmailSnapshots(
                    scoped.stream().map(KolDO::getId).toList());
            KolPageResult kolPage = buildKolPage(
                    scoped, detailParam, stageParam, scopedSnapshots, pageParam, sizeParam);
            kolRows = kolPage.rows();
            kolsPage = kolPage.pageMeta();
            recentActivity = buildRecentActivity(scoped, scopedSnapshots);
            platformDistribution = buildPlatformDistribution(scoped);
        }
        List<BoardMemberRowDto> memberRows = buildMemberRows(
                activeMembers, ownerId, includeInterns, windowed);
        List<String> availableMonths = buildAvailableMonths(rows);

        return new BoardSummaryDto(
                window.raw(),
                ownerId,
                includeInterns,
                kpi,
                funnel,
                distribution,
                kolRows,
                memberRows,
                platformDistribution,
                recentActivity,
                availableMonths,
                kolsPage
        );
    }

    private record KolPageResult(List<BoardKolDto> rows, PageMetaDto pageMeta) {
    }

    private List<String> buildAvailableMonths(List<KolDO> allKols) {
        Set<String> months = new TreeSet<>(Comparator.reverseOrder());
        for (KolDO kol : allKols) {
            if (kol.getFeishuOutreachAt() != null) {
                months.add(kol.getFeishuOutreachAt().format(YEAR_MONTH));
            }
        }
        return new ArrayList<>(months);
    }

    private List<BoardPlatformSegmentDto> buildPlatformDistribution(List<KolDO> scoped) {
        Map<Platform, Integer> counts = new EnumMap<>(Platform.class);
        for (Platform platform : PLATFORM_ORDER) {
            counts.put(platform, 0);
        }
        for (KolDO kol : scoped) {
            Platform platform = kol.getPrimaryPlatform() == null ? Platform.OTHER : kol.getPrimaryPlatform();
            counts.merge(platform, 1, Integer::sum);
        }
        List<BoardPlatformSegmentDto> segments = new ArrayList<>();
        for (Platform platform : PLATFORM_ORDER) {
            int count = counts.getOrDefault(platform, 0);
            if (count <= 0) {
                continue;
            }
            segments.add(new BoardPlatformSegmentDto(
                    platform.dbValue(),
                    PLATFORM_LABELS.getOrDefault(platform, platform.dbValue()),
                    count
            ));
        }
        return segments;
    }

    private List<BoardKolDto> buildRecentActivity(List<KolDO> scoped, Map<UUID, KolEmailSnapshot> snapshots) {
        return scoped.stream()
                .map(kol -> toBoardKol(kol, snapshots.getOrDefault(kol.getId(), KolEmailSnapshot.EMPTY)))
                .filter(kol -> kol.latestEmail() != null)
                .sorted(Comparator.comparing(
                        (BoardKolDto kol) -> kol.latestEmail().sentAt() == null
                                ? 0L
                                : kol.latestEmail().sentAt().toInstant().toEpochMilli()
                ).reversed())
                .limit(RECENT_ACTIVITY_LIMIT)
                .toList();
    }

    private List<BoardMemberRowDto> buildMemberRows(
            List<ProfileDO> activeMembers,
            UUID selectedOwnerId,
            boolean includeInterns,
            List<KolDO> windowedKols
    ) {
        List<ProfileDO> visibleMembers = BoardOwnerScope.visibleBoardMembers(
                selectedOwnerId, includeInterns, activeMembers);
        if (visibleMembers.isEmpty()) {
            return List.of();
        }

        Map<UUID, KolEmailSnapshot> snapshots = loadEmailSnapshots(
                windowedKols.stream().map(KolDO::getId).toList());
        boolean aggregateInterns = selectedOwnerId != null && includeInterns;

        List<BoardMemberRowDto> rows = new ArrayList<>();
        for (ProfileDO member : visibleMembers) {
            List<UUID> coveredMemberIds = new ArrayList<>();
            coveredMemberIds.add(member.getId());
            if (aggregateInterns) {
                coveredMemberIds.addAll(BoardOwnerScope.internIdsFor(member.getId(), activeMembers));
            }

            Set<UUID> coveredSet = Set.copyOf(coveredMemberIds);
            List<KolDO> memberKols = windowedKols.stream()
                    .filter(kol -> kol.getOwnerUserId() != null && coveredSet.contains(kol.getOwnerUserId()))
                    .toList();

            Map<KolStage, Integer> stageCounts = new EnumMap<>(KolStage.class);
            for (KolStage stage : StageCatalog.ALL_STAGES) {
                stageCounts.put(stage, 0);
            }
            int unread = 0;
            int unreplied = 0;
            for (KolDO kol : memberKols) {
                if (kol.getStage() != null) {
                    stageCounts.merge(kol.getStage(), 1, Integer::sum);
                }
                KolEmailSnapshot snapshot = snapshots.getOrDefault(kol.getId(), KolEmailSnapshot.EMPTY);
                unread += snapshot.unreadCount();
                if (needsReply(kol)) {
                    unreplied++;
                }
            }

            rows.add(new BoardMemberRowDto(
                    member.getId(),
                    member.getDisplayName(),
                    member.getRole(),
                    List.copyOf(coveredMemberIds),
                    StageCatalog.stageCountsJson(stageCounts),
                    memberKols.size(),
                    unread,
                    unreplied
            ));
        }
        return rows;
    }

    private KolPageResult buildKolPage(
            List<KolDO> scoped,
            String detailParam,
            String stageParam,
            Map<UUID, KolEmailSnapshot> snapshots,
            Integer pageParam,
            Integer sizeParam
    ) {
        String detail = BoardDetailFilter.normalizeDetail(detailParam);
        if (detail == null || scoped.isEmpty()) {
            return new KolPageResult(List.of(), null);
        }
        KolStage stageFilter = BoardDetailFilter.parseStage(stageParam);

        List<KolDO> filtered = new ArrayList<>();
        for (KolDO kol : scoped) {
            KolEmailSnapshot snap = snapshots.getOrDefault(kol.getId(), KolEmailSnapshot.EMPTY);
            boolean unreplied = needsReply(kol);
            if (!BoardDetailFilter.matchesDetail(kol, detail, snap.unreadCount(), unreplied)) {
                continue;
            }
            if (stageFilter != null && !stageFilter.equals(kol.getStage())) {
                continue;
            }
            filtered.add(kol);
        }

        filtered.sort(Comparator.comparing(
                (KolDO kol) -> {
                    EmailDO latest = snapshots.getOrDefault(kol.getId(), KolEmailSnapshot.EMPTY).latest();
                    return latest == null || latest.getSentAt() == null ? 0L : latest.getSentAt().toInstant().toEpochMilli();
                }
        ).reversed());

        int total = filtered.size();
        int page = pageParam == null || pageParam < 1 ? 1 : pageParam;
        int size = sizeParam == null || sizeParam < 1
                ? DEFAULT_KOL_PAGE_SIZE
                : Math.min(sizeParam, MAX_KOL_PAGE_SIZE);
        int fromIndex = (page - 1) * size;
        List<KolDO> slice = fromIndex >= total
                ? List.of()
                : filtered.subList(fromIndex, Math.min(fromIndex + size, total));

        List<BoardKolDto> rows = slice.stream()
                .map(kol -> toBoardKol(kol, snapshots.getOrDefault(kol.getId(), KolEmailSnapshot.EMPTY)))
                .toList();
        return new KolPageResult(rows, new PageMetaDto(page, size, total));
    }

    private Map<UUID, KolEmailSnapshot> loadEmailSnapshots(List<UUID> kolIds) {
        if (kolIds.isEmpty()) {
            return Map.of();
        }
        List<EmailDO> emailRows = emails.selectList(
                new LambdaQueryWrapper<EmailDO>()
                        .in(EmailDO::getKolId, kolIds)
                        .orderByDesc(EmailDO::getSentAt)
                        .last("LIMIT " + EMAIL_FETCH_LIMIT));

        Map<UUID, List<EmailDO>> byKol = new HashMap<>();
        for (EmailDO email : emailRows) {
            if (email.getKolId() == null) {
                continue;
            }
            byKol.computeIfAbsent(email.getKolId(), ignored -> new ArrayList<>()).add(email);
        }

        Map<UUID, KolEmailSnapshot> snapshots = new HashMap<>();
        for (UUID kolId : kolIds) {
            List<EmailDO> kolEmails = byKol.getOrDefault(kolId, List.of());
            EmailDO latest = kolEmails.isEmpty() ? null : kolEmails.getFirst();
            int unread = (int) kolEmails.stream()
                    .filter(e -> e.getDirection() == EmailDirection.INBOUND && !Boolean.TRUE.equals(e.getIsRead()))
                    .count();
            snapshots.put(kolId, new KolEmailSnapshot(latest, unread));
        }
        return snapshots;
    }

    private static BoardKolDto toBoardKol(KolDO kol, KolEmailSnapshot snapshot) {
        EmailDO latest = snapshot.latest();
        BoardKolLatestEmailDto latestDto = latest == null
                ? null
                : new BoardKolLatestEmailDto(
                        latest.getSubject(),
                        latest.getAiSummary(),
                        latest.getAiPriority(),
                        latest.getDirection() == null ? null : latest.getDirection().name().toLowerCase(),
                        latest.getSentAt()
                );
        return new BoardKolDto(
                kol.getId(),
                kol.getName() == null || kol.getName().isBlank() ? kol.getEmail() : kol.getName(),
                kol.getEmail(),
                kol.getStage() == null ? null : kol.getStage().apiValue(),
                kol.getPrimaryPlatform() == null ? null : kol.getPrimaryPlatform().dbValue(),
                kol.getType(),
                snapshot.unreadCount(),
                needsReply(kol),
                latestDto
        );
    }

    private int countUnreadInboundEmails(List<UUID> kolIds) {
        if (kolIds.isEmpty()) {
            return 0;
        }
        Long count = emails.selectCount(
                new LambdaQueryWrapper<EmailDO>()
                        .in(EmailDO::getKolId, kolIds)
                        .eq(EmailDO::getDirection, EmailDirection.INBOUND)
                        .eq(EmailDO::getIsRead, false));
        return count == null ? 0 : count.intValue();
    }

    /**
     * Needs reply: latest activity is inbound and not manually resolved.
     */
    static boolean needsReply(KolDO kol) {
        if (Boolean.TRUE.equals(kol.getReplyResolved())) {
            return false;
        }
        if (kol.getLastInboundAt() == null) {
            return false;
        }
        if (kol.getLastOutboundAt() != null && kol.getLastOutboundAt().isAfter(kol.getLastInboundAt())) {
            return false;
        }
        return true;
    }

    private record KolEmailSnapshot(EmailDO latest, int unreadCount) {
        private static final KolEmailSnapshot EMPTY = new KolEmailSnapshot(null, 0);
    }
}
