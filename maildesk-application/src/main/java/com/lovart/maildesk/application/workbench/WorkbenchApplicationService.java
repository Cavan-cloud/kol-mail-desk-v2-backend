package com.lovart.maildesk.application.workbench;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.KolDetailDto;
import com.lovart.maildesk.application.dto.PageMetaDto;
import com.lovart.maildesk.application.dto.WorkbenchKolDto;
import com.lovart.maildesk.application.dto.WorkbenchResponseDto;
import com.lovart.maildesk.application.dto.WorkbenchSidebarStatsDto;
import com.lovart.maildesk.application.support.EntityMappers;
import com.lovart.maildesk.application.support.StageCatalog;
import com.lovart.maildesk.application.support.WorkbenchRules;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.enums.KolStatus;
import com.lovart.maildesk.common.exception.BusinessException;
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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkbenchApplicationService {

    private static final int EMAIL_FETCH_LIMIT = 500;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final KolMapper kols;
    private final EmailMapper emails;
    private final ProfileMapper profiles;

    public WorkbenchApplicationService(KolMapper kols, EmailMapper emails, ProfileMapper profiles) {
        this.kols = kols;
        this.emails = emails;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public WorkbenchResponseDto getWorkbench(
            UUID userId,
            String view,
            String stage,
            String query,
            int page,
            int size
    ) {
        String viewMode = normalizeView(view);
        int pageNum = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        List<KolDO> kolRows = loadKolsForView(viewMode, userId);
        long scopedTotal = countKolsForView(viewMode, userId);

        List<EmailDO> emailRows = emails.selectList(
                new LambdaQueryWrapper<EmailDO>()
                        .eq(EmailDO::getUserId, userId)
                        .orderByDesc(EmailDO::getSentAt)
                        .last("LIMIT " + EMAIL_FETCH_LIMIT));

        Map<UUID, String> ownerNames = loadOwnerNames();
        Map<UUID, List<EmailDO>> emailsByKol = groupEmailsByKol(emailRows);
        List<WorkbenchKolDto> base = kolRows.stream()
                .map(kol -> toWorkbenchKol(kol, emailsByKol, ownerNames))
                .toList();

        String stageFilter = stage == null || stage.isBlank() ? "all" : stage.trim().toLowerCase(Locale.ROOT);
        List<WorkbenchKolDto> filtered = base.stream()
                .filter(kol -> matchesStageFilter(kol, stageFilter))
                .filter(kol -> matchesQuery(kol, query))
                .sorted(Comparator.comparing(
                        (WorkbenchKolDto k) -> k.latestEmail() == null ? 0L : k.latestEmail().sentAt().toInstant().toEpochMilli()
                ).reversed())
                .toList();

        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, filtered.size());
        List<WorkbenchKolDto> pageData = from >= filtered.size()
                ? List.of()
                : filtered.subList(from, to);

        WorkbenchSidebarStatsDto sidebar = buildSidebar(base, scopedTotal);
        PageMetaDto pageMeta = new PageMetaDto(pageNum, pageSize, filtered.size());
        return new WorkbenchResponseDto(pageData, sidebar, pageMeta);
    }

    @Transactional(readOnly = true)
    public KolDetailDto getKolDetail(UUID userId, UUID kolId) {
        KolDO kol = kols.selectById(kolId);
        if (kol == null) {
            throw new BusinessException("NOT_FOUND", "达人不存在");
        }
        List<EmailDO> timeline = emails.selectList(
                new LambdaQueryWrapper<EmailDO>()
                        .eq(EmailDO::getKolId, kolId)
                        .eq(EmailDO::getUserId, userId)
                        .orderByDesc(EmailDO::getSentAt));

        String ownerName = resolveOwnerName(kol.getOwnerUserId());
        return new KolDetailDto(
                EntityMappers.toKolDto(kol),
                ownerName,
                timeline.stream().map(EntityMappers::toEmailDto).toList()
        );
    }

    private List<KolDO> loadKolsForView(String viewMode, UUID userId) {
        LambdaQueryWrapper<KolDO> wrapper = new LambdaQueryWrapper<KolDO>()
                .orderByDesc(KolDO::getCreatedAt);
        applyViewFilter(wrapper, viewMode, userId);
        return kols.selectList(wrapper);
    }

    private long countKolsForView(String viewMode, UUID userId) {
        LambdaQueryWrapper<KolDO> wrapper = new LambdaQueryWrapper<>();
        applyViewFilter(wrapper, viewMode, userId);
        Long count = kols.selectCount(wrapper);
        return count == null ? 0 : count;
    }

    private void applyViewFilter(LambdaQueryWrapper<KolDO> wrapper, String viewMode, UUID userId) {
        if ("mine".equals(viewMode)) {
            wrapper.eq(KolDO::getOwnerUserId, userId);
        } else if ("pool".equals(viewMode)) {
            wrapper.in(KolDO::getStatus, KolStatus.UNASSIGNED, KolStatus.ORPHANED);
        }
    }

    private WorkbenchKolDto toWorkbenchKol(
            KolDO kol,
            Map<UUID, List<EmailDO>> emailsByKol,
            Map<UUID, String> ownerNames
    ) {
        List<EmailDO> kolEmails = emailsByKol.getOrDefault(kol.getId(), List.of());
        EmailDO latest = kolEmails.isEmpty() ? null : kolEmails.getFirst();
        int unread = (int) kolEmails.stream()
                .filter(e -> e.getDirection() == EmailDirection.INBOUND && !Boolean.TRUE.equals(e.getIsRead()))
                .count();
        boolean replyResolved = Boolean.TRUE.equals(kol.getReplyResolved());
        boolean unreplied = WorkbenchRules.needsMyReply(latest, replyResolved);
        boolean awaiting = WorkbenchRules.awaitingTheirReply(latest);

        String ownerName = kol.getOwnerUserId() == null
                ? "待认领"
                : ownerNames.getOrDefault(kol.getOwnerUserId(), "未知成员");
        return EntityMappers.toWorkbenchKolDto(kol, ownerName, latest, unread, unreplied, awaiting);
    }

    private static Map<UUID, List<EmailDO>> groupEmailsByKol(List<EmailDO> emailRows) {
        Map<UUID, List<EmailDO>> map = new HashMap<>();
        for (EmailDO email : emailRows) {
            if (email.getKolId() == null) {
                continue;
            }
            map.computeIfAbsent(email.getKolId(), ignored -> new ArrayList<>()).add(email);
        }
        return map;
    }

    private Map<UUID, String> loadOwnerNames() {
        return profiles.selectList(new LambdaQueryWrapper<ProfileDO>())
                .stream()
                .collect(Collectors.toMap(ProfileDO::getId, ProfileDO::getDisplayName, (a, b) -> a));
    }

    private String resolveOwnerName(UUID ownerUserId) {
        if (ownerUserId == null) {
            return "待认领";
        }
        ProfileDO profile = profiles.selectById(ownerUserId);
        return profile == null ? "未知成员" : profile.getDisplayName();
    }

    private WorkbenchSidebarStatsDto buildSidebar(List<WorkbenchKolDto> base, long scopedTotal) {
        Map<KolStage, Integer> stageCounts = new EnumMap<>(KolStage.class);
        for (KolStage stage : StageCatalog.ALL_STAGES) {
            stageCounts.put(stage, 0);
        }
        int unread = 0;
        int unreplied = 0;
        for (WorkbenchKolDto kol : base) {
            if (kol.stage() != null) {
                try {
                    KolStage stage = KolStage.valueOf(kol.stage().toUpperCase(Locale.ROOT));
                    stageCounts.merge(stage, 1, Integer::sum);
                } catch (IllegalArgumentException ignored) {
                    // skip unknown stage strings
                }
            }
            unread += kol.unreadCount();
            if (kol.unreplied()) {
                unreplied++;
            }
        }
        return new WorkbenchSidebarStatsDto(
                (int) scopedTotal,
                unread,
                unreplied,
                StageCatalog.stageCountsJson(stageCounts)
        );
    }

    private static boolean matchesStageFilter(WorkbenchKolDto kol, String stageFilter) {
        if ("all".equals(stageFilter)) {
            return true;
        }
        if ("unread".equals(stageFilter)) {
            return kol.unreadCount() > 0;
        }
        if ("unreplied".equals(stageFilter)) {
            return kol.unreplied();
        }
        return stageFilter.equals(kol.stage());
    }

    private static boolean matchesQuery(WorkbenchKolDto kol, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return true;
        }
        String haystack = String.join(" ",
                nullToEmpty(kol.name()),
                nullToEmpty(kol.email()),
                nullToEmpty(kol.handle()),
                nullToEmpty(kol.ownerName()),
                nullToEmpty(kol.notes()),
                kol.latestEmail() == null ? "" : nullToEmpty(kol.latestEmail().subject()),
                kol.latestEmail() == null ? "" : nullToEmpty(kol.latestEmail().aiSummary())
        ).toLowerCase(Locale.ROOT);
        return haystack.contains(q);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeView(String view) {
        if (view == null || view.isBlank()) {
            return "mine";
        }
        String v = view.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "pool", "all" -> v;
            default -> "mine";
        };
    }
}
