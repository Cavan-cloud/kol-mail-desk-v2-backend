package com.lovart.maildesk.application.sync.gmail;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.observability.MaildeskMetrics;
import com.lovart.maildesk.common.context.UserContext;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.exception.GmailIntegrationException;
import com.lovart.maildesk.domain.credential.GoogleAccessToken;
import com.lovart.maildesk.domain.credential.GoogleCredentialPort;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.gmail.GmailClient;
import com.lovart.maildesk.domain.gmail.GmailFullMessage;
import com.lovart.maildesk.domain.gmail.GmailHistoryPage;
import com.lovart.maildesk.domain.gmail.GmailMessageListPage;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class GmailSyncService {

    private static final Logger log = LoggerFactory.getLogger(GmailSyncService.class);
    private static final int FETCH_CONCURRENCY = 4;

    private final GmailClient gmailClient;
    private final GoogleCredentialPort credentials;
    private final ProfileMapper profiles;
    private final EmailMapper emails;
    private final GmailPersistService persistService;
    private final GmailEmailClassificationService classificationService;
    private final MaildeskMetrics metrics;

    public GmailSyncService(
            GmailClient gmailClient,
            GoogleCredentialPort credentials,
            ProfileMapper profiles,
            EmailMapper emails,
            GmailPersistService persistService,
            GmailEmailClassificationService classificationService,
            MaildeskMetrics metrics) {
        this.gmailClient = gmailClient;
        this.credentials = credentials;
        this.profiles = profiles;
        this.emails = emails;
        this.persistService = persistService;
        this.classificationService = classificationService;
        this.metrics = metrics;
    }

    public GmailSyncResult sync(UUID userId, GmailSyncOptions options) {
        long started = System.nanoTime();
        String modeTag = "unknown";
        try {
            ProfileDO profile = profiles.selectById(userId);
            if (profile == null) {
                metrics.recordGmailSync(elapsed(started), modeTag, "not_configured");
                return GmailSyncResult.notConfigured("用户不存在");
            }
            Optional<GoogleAccessToken> tokenOpt = credentials.resolveAccessToken(userId);
            if (tokenOpt.isEmpty()) {
                metrics.recordGmailSync(elapsed(started), modeTag, "not_configured");
                return GmailSyncResult.notConfigured("未找到 Gmail refresh token，请重新登录并授权 Gmail 权限。");
            }
            String accessToken = tokenOpt.get().accessToken();
            String ownEmail = profile.getEmail() == null ? "" : profile.getEmail();

            UserContext.setUserId(userId);
            try {
                List<String> messageIds;
                String listMode;
                String nextPageToken = null;

                if (options.historyMode() || options.forceRecent()
                        || profile.getLastSyncedHistoryId() == null
                        || profile.getLastSyncedHistoryId().isBlank()) {
                    GmailMessageListPage page = gmailClient.listRecentMessages(
                            accessToken, options.maxResults(), options.historyDays(), options.pageToken());
                    messageIds = page.messageIds();
                    listMode = page.mode();
                    nextPageToken = page.nextPageToken();
                } else {
                    GmailHistoryPage history = gmailClient.listIncrementalMessageIds(
                            accessToken, profile.getLastSyncedHistoryId(), options.maxResults());
                    if (history.messageIds().isEmpty() && "recent_fallback".equals(history.mode())) {
                        GmailMessageListPage page = gmailClient.listRecentMessages(
                                accessToken, options.maxResults(), options.historyDays(), options.pageToken());
                        messageIds = page.messageIds();
                        listMode = page.mode();
                        nextPageToken = page.nextPageToken();
                    } else {
                        messageIds = history.messageIds();
                        listMode = history.mode();
                    }
                }
                modeTag = listMode;

                Set<String> knownIds = loadKnownMessageIds(userId, messageIds);
                List<GmailSyncMessageDraft> drafts = fetchAndParse(
                        accessToken, messageIds, knownIds, ownEmail);
                String nextHistoryId = maxHistoryId(profile.getLastSyncedHistoryId(), drafts);

                GmailPersistService.PersistResult persistResult =
                        persistService.persist(userId, profile.getFeishuOperatorName(), drafts);
                updateProfileCursors(profile, options, nextPageToken, nextHistoryId);

                metrics.recordGmailSync(elapsed(started), listMode, "synced");
                return new GmailSyncResult(
                        "synced",
                        null,
                        drafts.size(),
                        listMode,
                        nextPageToken,
                        nextHistoryId,
                        persistResult.insertedEmails());
            } finally {
                UserContext.clear();
            }
        } catch (RuntimeException ex) {
            metrics.recordGmailSync(elapsed(started), modeTag, "error");
            throw ex;
        }
    }

    private static Duration elapsed(long startedNanos) {
        return Duration.ofNanos(System.nanoTime() - startedNanos);
    }

    public List<UUID> listUsersDueForIncrementalSync(int limit) {
        List<ProfileDO> rows = profiles.selectList(new LambdaQueryWrapper<ProfileDO>()
                .inSql(
                        ProfileDO::getId,
                        "SELECT user_id FROM integration_credentials WHERE type = 'google' AND deleted_at IS NULL")
                .ne(ProfileDO::getStatus, "departed")
                .orderByAsc(ProfileDO::getLastSyncedAt)
                .last("LIMIT " + Math.max(limit, 1)));
        return rows.stream().map(ProfileDO::getId).toList();
    }

    private Set<String> loadKnownMessageIds(UUID userId, List<String> messageIds) {
        if (messageIds.isEmpty()) {
            return Set.of();
        }
        List<EmailDO> rows = emails.selectList(new LambdaQueryWrapper<EmailDO>()
                .eq(EmailDO::getUserId, userId)
                .in(EmailDO::getGmailMessageId, messageIds));
        Set<String> known = new HashSet<>();
        for (EmailDO row : rows) {
            known.add(row.getGmailMessageId());
        }
        return known;
    }

    private List<GmailSyncMessageDraft> fetchAndParse(
            String accessToken, List<String> messageIds, Set<String> knownIds, String ownEmail) {
        if (messageIds.isEmpty()) {
            return List.of();
        }
        int workers = Math.min(FETCH_CONCURRENCY, messageIds.size());
        try (ExecutorService executor = Executors.newFixedThreadPool(workers)) {
            List<Future<GmailSyncMessageDraft>> futures = new ArrayList<>();
            for (String id : messageIds) {
                futures.add(executor.submit(() -> fetchOne(accessToken, id, knownIds, ownEmail)));
            }
            List<GmailSyncMessageDraft> drafts = new ArrayList<>();
            for (Future<GmailSyncMessageDraft> future : futures) {
                try {
                    GmailSyncMessageDraft draft = future.get();
                    if (draft != null) {
                        drafts.add(draft);
                    }
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof GmailIntegrationException gmailEx
                            && gmailEx.getMessage() != null
                            && gmailEx.getMessage().contains("not found")) {
                        continue;
                    }
                    if (cause instanceof RuntimeException runtime) {
                        throw runtime;
                    }
                    throw new GmailIntegrationException("Gmail message fetch failed", cause);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new GmailIntegrationException("Gmail sync interrupted");
                }
            }
            return drafts;
        }
    }

    private GmailSyncMessageDraft fetchOne(
            String accessToken, String messageId, Set<String> knownIds, String ownEmail) {
        try {
            GmailFullMessage parsed = gmailClient.getMessage(accessToken, messageId);
            EmailDirection direction = parsed.fromEmail().equalsIgnoreCase(ownEmail.trim())
                    ? EmailDirection.OUTBOUND
                    : EmailDirection.INBOUND;
            String counterparty = GmailCounterpartyEmail.resolve(
                    direction,
                    parsed.fromEmail(),
                    parsed.toEmails(),
                    parsed.ccEmails(),
                    ownEmail);
            boolean aiSkipped = knownIds.contains(messageId);
            GmailAiFallback.GmailAiFields ai = aiSkipped
                    ? GmailAiFallback.skippedExisting()
                    : classificationService.classify(parsed, direction);
            return new GmailSyncMessageDraft(
                    parsed,
                    direction,
                    counterparty,
                    ai.stageSignal(),
                    ai.priority(),
                    ai.summary(),
                    ai.bodyZh(),
                    ai.suggestedAction(),
                    ai.aiError(),
                    aiSkipped);
        } catch (GmailIntegrationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("not found")) {
                return null;
            }
            throw ex;
        }
    }

    private void updateProfileCursors(
            ProfileDO profile, GmailSyncOptions options, String nextPageToken, String nextHistoryId) {
        ProfileDO patch = new ProfileDO();
        patch.setId(profile.getId());
        patch.setLastSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
        boolean historyComplete = nextPageToken == null || nextPageToken.isBlank();
        if (historyComplete && nextHistoryId != null && !nextHistoryId.isBlank()) {
            patch.setLastSyncedHistoryId(nextHistoryId);
        }
        profiles.updateById(patch);
    }

    private static String maxHistoryId(String current, List<GmailSyncMessageDraft> drafts) {
        BigInteger max = toBigInt(current);
        for (GmailSyncMessageDraft draft : drafts) {
            BigInteger candidate = toBigInt(draft.parsed().historyId());
            if (candidate != null && (max == null || candidate.compareTo(max) >= 0)) {
                max = candidate;
            }
        }
        return max == null ? current : max.toString();
    }

    private static BigInteger toBigInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigInteger(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
