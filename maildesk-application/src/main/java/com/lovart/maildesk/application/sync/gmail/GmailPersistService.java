package com.lovart.maildesk.application.sync.gmail;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.enums.KolStatus;
import com.lovart.maildesk.common.feishu.FeishuCellExtractor;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Batched Gmail persist with Feishu-backed guard. Ported from legacy {@code lib/gmail/persist.ts}.
 */
@Service
public class GmailPersistService {

    private static final String SOURCE_FEISHU = "feishu";
    private static final String SOURCE_GMAIL = "gmail";

    private final KolMapper kols;
    private final EmailMapper emails;
    private final ObjectMapper objectMapper;

    public GmailPersistService(KolMapper kols, EmailMapper emails, ObjectMapper objectMapper) {
        this.kols = kols;
        this.emails = emails;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public PersistResult persist(UUID userId, String operatorName, List<GmailSyncMessageDraft> messages) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<IndexedDraft> indexed = new ArrayList<>();
        for (GmailSyncMessageDraft draft : messages) {
            String normalized = normalizeEmail(draft.counterpartyEmail());
            if (normalized.isBlank()) {
                continue;
            }
            indexed.add(new IndexedDraft(draft, normalized));
        }
        if (indexed.isEmpty()) {
            return new PersistResult(0, 0);
        }

        List<String> orderedEmails = new ArrayList<>();
        Map<String, TouchAggregate> touchByEmail = new LinkedHashMap<>();
        for (IndexedDraft item : indexed) {
            touchByEmail.computeIfAbsent(item.normalizedEmail, email -> {
                orderedEmails.add(email);
                return new TouchAggregate();
            });
            TouchAggregate agg = touchByEmail.get(item.normalizedEmail);
            String sentAt = item.draft.parsed().sentAt().toString();
            if (item.draft.direction() == EmailDirection.INBOUND) {
                agg.lastInbound = sentAt;
            } else {
                agg.lastOutbound = sentAt;
            }
        }

        List<String> allMessageIds =
                indexed.stream().map(i -> i.draft.parsed().gmailMessageId()).distinct().toList();
        Map<String, List<KolDO>> candidatesByEmail = loadCandidates(orderedEmails);
        Map<String, UUID> existingEmailIdByMessage = loadExistingEmails(userId, allMessageIds);

        String normalizedOperator = FeishuCellExtractor.normalizeOperatorName(operatorName == null ? "" : operatorName);
        Map<String, UUID> kolIdByEmail = new HashMap<>();
        Map<UUID, UUID> pendingOwnerClaimByKolId = new HashMap<>();
        List<KolDO> newKolRows = new ArrayList<>();

        for (String email : orderedEmails) {
            List<KolDO> rows = candidatesByEmail.getOrDefault(email, List.of());
            if (!isFeishuBacked(rows)) {
                continue;
            }
            TouchAggregate agg = touchByEmail.get(email);
            KolDO owned = rows.stream().filter(r -> userId.equals(r.getOwnerUserId())).findFirst().orElse(null);
            if (owned != null) {
                kolIdByEmail.put(email, owned.getId());
                continue;
            }
            KolDO claimable = !normalizedOperator.isEmpty()
                    ? rows.stream()
                            .filter(r -> r.getOwnerUserId() == null)
                            .filter(r -> FeishuCellExtractor.normalizeOperatorName(r.getFeishuOperatorName())
                                    .equals(normalizedOperator))
                            .findFirst()
                            .orElse(null)
                    : null;
            if (claimable != null) {
                kolIdByEmail.put(email, claimable.getId());
                pendingOwnerClaimByKolId.put(claimable.getId(), userId);
                continue;
            }
            KolDO created = newKol(email, operatorName, userId, agg);
            newKolRows.add(created);
        }

        for (KolDO row : newKolRows) {
            kols.insert(row);
            kolIdByEmail.put(normalizeEmail(row.getEmail()), row.getId());
        }

        int upsertedKols = 0;
        int insertedEmails = 0;
        Set<String> seenNewMessageIds = new HashSet<>();
        Set<UUID> kolIdsWithNewInbound = new HashSet<>();

        for (IndexedDraft item : indexed) {
            UUID kolId = kolIdByEmail.get(item.normalizedEmail);
            if (kolId == null) {
                continue;
            }
            upsertedKols++;

            UUID existingId = existingEmailIdByMessage.get(item.draft.parsed().gmailMessageId());
            if (existingId != null) {
                if (item.draft.aiClassificationSkipped()) {
                    updateExistingMetadata(existingId, item.draft, kolId, userId);
                } else {
                    EmailDO fields = toEmailFields(item.draft, kolId, userId, now);
                    fields.setId(existingId);
                    emails.updateById(fields);
                }
                insertedEmails++;
            } else if (!seenNewMessageIds.contains(item.draft.parsed().gmailMessageId())) {
                EmailDO fields = toEmailFields(item.draft, kolId, userId, now);
                seenNewMessageIds.add(item.draft.parsed().gmailMessageId());
                boolean unread = item.draft.parsed().labelIds().contains("UNREAD");
                fields.setIsRead(item.draft.direction() == EmailDirection.OUTBOUND || !unread);
                emails.insert(fields);
                if (item.draft.direction() == EmailDirection.INBOUND) {
                    kolIdsWithNewInbound.add(kolId);
                }
                insertedEmails++;
            }
        }

        List<KolTouchUpdate> kolTouchUpdates = new ArrayList<>();
        for (String email : orderedEmails) {
            UUID kolId = kolIdByEmail.get(email);
            if (kolId == null) {
                continue;
            }
            TouchAggregate agg = touchByEmail.get(email);
            Map<String, Object> patch = touchPatch(agg, now, kolIdsWithNewInbound.contains(kolId));
            UUID claimOwner = pendingOwnerClaimByKolId.get(kolId);
            if (claimOwner != null) {
                patch.put("owner_user_id", claimOwner);
            }
            kolTouchUpdates.add(new KolTouchUpdate(kolId, patch));
        }
        for (KolTouchUpdate update : kolTouchUpdates) {
            UpdateWrapper<KolDO> wrapper = new UpdateWrapper<>();
            update.patch().forEach(wrapper::set);
            wrapper.eq("id", update.kolId());
            kols.update(null, wrapper);
        }

        return new PersistResult(insertedEmails, upsertedKols);
    }

    private Map<String, List<KolDO>> loadCandidates(List<String> orderedEmails) {
        if (orderedEmails.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<KolDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> {
            boolean first = true;
            for (String email : orderedEmails) {
                if (first) {
                    w.apply("normalized_email = {0}", email);
                    first = false;
                } else {
                    w.or().apply("normalized_email = {0}", email);
                }
            }
        });
        Map<String, List<KolDO>> map = new HashMap<>();
        for (KolDO row : kols.selectList(wrapper)) {
            String key = normalizeEmail(row.getEmail());
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private Map<String, UUID> loadExistingEmails(UUID userId, List<String> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        List<EmailDO> rows = emails.selectList(new LambdaQueryWrapper<EmailDO>()
                .eq(EmailDO::getUserId, userId)
                .in(EmailDO::getGmailMessageId, messageIds));
        Map<String, UUID> map = new HashMap<>();
        for (EmailDO row : rows) {
            map.putIfAbsent(row.getGmailMessageId(), row.getId());
        }
        return map;
    }

    private static boolean isFeishuBacked(List<KolDO> rows) {
        return rows.stream()
                .anyMatch(row -> SOURCE_FEISHU.equals(row.getSource()) || row.getFeishuRecordId() != null);
    }

    private static Map<String, Object> touchPatch(TouchAggregate agg, OffsetDateTime now, boolean clearReplyResolved) {
        Map<String, Object> patch = new LinkedHashMap<>();
        if (agg.lastInbound != null) {
            patch.put("last_inbound_at", OffsetDateTime.parse(agg.lastInbound));
            if (clearReplyResolved) {
                patch.put("reply_resolved", false);
            }
        }
        if (agg.lastOutbound != null) {
            patch.put("last_outbound_at", OffsetDateTime.parse(agg.lastOutbound));
        }
        patch.put("updated_at", now);
        return patch;
    }

    private static KolDO newKol(String email, String operatorName, UUID userId, TouchAggregate agg) {
        KolDO row = new KolDO();
        row.setEmail(email);
        row.setFeishuOperatorName(operatorName);
        row.setName(email.split("@")[0]);
        row.setHandle("@" + email.split("@")[0]);
        row.setStage(KolStage.OUTREACH);
        row.setStatus(KolStatus.ACTIVE);
        row.setSource(SOURCE_GMAIL);
        row.setOwnerUserId(userId);
        if (agg.lastInbound != null) {
            row.setLastInboundAt(OffsetDateTime.parse(agg.lastInbound));
            row.setReplyResolved(false);
        }
        if (agg.lastOutbound != null) {
            row.setLastOutboundAt(OffsetDateTime.parse(agg.lastOutbound));
        }
        return row;
    }

    /**
     * Refreshes Gmail-derived fields on re-sync without clobbering AI columns or manual read state
     * (legacy {@code persist.ts} update path).
     */
    private void updateExistingMetadata(UUID existingId, GmailSyncMessageDraft item, UUID kolId, UUID userId) {
        EmailDO patch = new EmailDO();
        patch.setId(existingId);
        patch.setGmailThreadId(item.parsed().gmailThreadId());
        patch.setKolId(kolId);
        patch.setUserId(userId);
        patch.setDirection(item.direction());
        patch.setFromEmail(item.parsed().fromEmail());
        patch.setToEmails(item.parsed().toEmails());
        patch.setCcEmails(item.parsed().ccEmails());
        patch.setSubject(item.parsed().subject());
        patch.setBodyText(item.parsed().bodyText());
        patch.setBodyHtml(item.parsed().bodyHtml());
        patch.setAttachmentNames(item.parsed().attachmentNames());
        patch.setHasAttachments(item.parsed().hasAttachments());
        patch.setSentAt(item.parsed().sentAt());
        emails.updateById(patch);
    }

    private EmailDO toEmailFields(GmailSyncMessageDraft item, UUID kolId, UUID userId, OffsetDateTime now) {
        ObjectNode extracted = objectMapper.createObjectNode();
        EmailDO row = new EmailDO();
        row.setGmailMessageId(item.parsed().gmailMessageId());
        row.setGmailThreadId(item.parsed().gmailThreadId());
        row.setKolId(kolId);
        row.setUserId(userId);
        row.setDirection(item.direction());
        row.setFromEmail(item.parsed().fromEmail());
        row.setToEmails(item.parsed().toEmails());
        row.setCcEmails(item.parsed().ccEmails());
        row.setSubject(item.parsed().subject());
        row.setBodyText(item.parsed().bodyText());
        row.setBodyHtml(item.parsed().bodyHtml());
        row.setBodyZh(item.bodyZh());
        row.setAttachmentNames(item.parsed().attachmentNames());
        row.setHasAttachments(item.parsed().hasAttachments());
        row.setSentAt(item.parsed().sentAt());
        row.setAiStageSignal(item.aiStageSignal());
        row.setAiPriority(item.aiPriority());
        row.setAiSummary(item.aiSummary());
        row.setAiSuggestedAction(item.aiSuggestedAction());
        row.setAiExtractedFields(extracted);
        row.setAiError(item.aiError());
        row.setAiProcessedAt(now);
        return row;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public record PersistResult(int insertedEmails, int upsertedKols) {
    }

    private record IndexedDraft(GmailSyncMessageDraft draft, String normalizedEmail) {
    }

    private record KolTouchUpdate(UUID kolId, Map<String, Object> patch) {
    }

    private static final class TouchAggregate {
        private String lastInbound;
        private String lastOutbound;
    }
}
