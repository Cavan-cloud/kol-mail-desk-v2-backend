package com.lovart.maildesk.application.support;

import com.lovart.maildesk.application.dto.EmailDto;
import com.lovart.maildesk.application.dto.EmailTemplateDto;
import com.lovart.maildesk.application.dto.KolDto;
import com.lovart.maildesk.application.dto.ProfileDto;
import com.lovart.maildesk.application.dto.ScheduledEmailDto;
import com.lovart.maildesk.application.dto.TeamMemberDto;
import com.lovart.maildesk.application.dto.WorkbenchKolDto;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.enums.KolStatus;
import com.lovart.maildesk.common.enums.Platform;
import com.lovart.maildesk.common.enums.UserRole;
import com.lovart.maildesk.common.enums.UserStatus;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.scheduled.entity.ScheduledEmailDO;
import com.lovart.maildesk.domain.template.entity.EmailTemplateDO;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

/**
 * Maps domain entities to OpenAPI-aligned response DTOs.
 */
public final class EntityMappers {

    private EntityMappers() {
    }

    public static KolDto toKolDto(KolDO kol) {
        if (kol == null) {
            return null;
        }
        return new KolDto(
                kol.getId(),
                kol.getEmail(),
                kol.getName(),
                kol.getHandle(),
                enumToJson(kol.getPrimaryPlatform()),
                kol.getType(),
                kol.getExternalProfileUrl(),
                kol.getSource(),
                kol.getFeishuRecordId(),
                localDateToOffsetDateTime(kol.getFeishuOutreachAt()),
                enumToJson(kol.getStage()),
                enumToJson(kol.getStatus()),
                kol.getOwnerUserId(),
                kol.getLastInboundAt(),
                kol.getLastOutboundAt(),
                kol.getAgreedPrice(),
                kol.getBrandQuote(),
                kol.getFinalCooperationPrice(),
                kol.getAgreedDeadline(),
                kol.getNotes(),
                Boolean.TRUE.equals(kol.getReplyResolved()),
                Boolean.TRUE.equals(kol.getStageOverride()),
                kol.getCreatedAt(),
                kol.getUpdatedAt()
        );
    }

    public static EmailDto toEmailDto(EmailDO email) {
        if (email == null) {
            return null;
        }
        return new EmailDto(
                email.getId(),
                email.getKolId(),
                email.getUserId(),
                enumToJson(email.getDirection()),
                email.getFromEmail(),
                copyList(email.getToEmails()),
                copyList(email.getCcEmails()),
                email.getSubject(),
                email.getBodyText(),
                email.getBodyHtml(),
                email.getBodyZh(),
                Boolean.TRUE.equals(email.getHasAttachments()),
                copyList(email.getAttachmentNames()),
                email.getSentAt(),
                enumToJson(email.getAiStageSignal()),
                email.getAiPriority(),
                email.getAiSummary(),
                email.getAiSuggestedAction(),
                Boolean.TRUE.equals(email.getIsRead()),
                email.getReadAt(),
                email.getCreatedAt()
        );
    }

    public static ProfileDto toProfileDto(ProfileDO profile, boolean gmailAuthorized) {
        if (profile == null) {
            return null;
        }
        return new ProfileDto(
                profile.getId(),
                profile.getDisplayName(),
                profile.getEmail(),
                roleToJson(profile.getRole()),
                statusToJson(profile.getStatus()),
                profile.getMentorUserId(),
                profile.getFeishuOperatorName(),
                gmailAuthorized,
                profile.getLastSyncedAt(),
                profile.getCreatedAt()
        );
    }

    public static TeamMemberDto toTeamMemberDto(
            ProfileDO profile,
            boolean gmailAuthorized,
            int ownedKolCount,
            int activeKolCount,
            int closedKolCount,
            int stalledKolCount) {
        if (profile == null) {
            return null;
        }
        return new TeamMemberDto(
                profile.getId(),
                profile.getDisplayName(),
                profile.getEmail(),
                roleToJson(profile.getRole()),
                statusToJson(profile.getStatus()),
                profile.getMentorUserId(),
                profile.getFeishuOperatorName(),
                gmailAuthorized,
                profile.getLastSyncedAt(),
                profile.getCreatedAt(),
                ownedKolCount,
                activeKolCount,
                closedKolCount,
                stalledKolCount
        );
    }

    public static EmailTemplateDto toEmailTemplateDto(EmailTemplateDO template) {
        if (template == null) {
            return null;
        }
        return new EmailTemplateDto(
                template.getId(),
                template.getName(),
                template.getScenario(),
                template.getSubject(),
                template.getBody(),
                template.getUsedCount() != null ? template.getUsedCount() : 0,
                template.getLastUsedAt(),
                template.getCreatedBy(),
                template.getCreatedAt()
        );
    }

    public static ScheduledEmailDto toScheduledEmailDto(ScheduledEmailDO scheduled, String kolName) {
        if (scheduled == null) {
            return null;
        }
        return new ScheduledEmailDto(
                scheduled.getId(),
                scheduled.getKolId(),
                kolName,
                scheduled.getTemplateId(),
                scheduled.getToEmail(),
                copyList(scheduled.getCcEmails()),
                scheduled.getSubject(),
                scheduled.getEnglishBody(),
                scheduled.getEnglishBodyHtml(),
                scheduled.getChineseDraft(),
                scheduled.getScheduledAt(),
                scheduled.getStatus(),
                scheduled.getAttemptCount() != null ? scheduled.getAttemptCount() : 0,
                scheduled.getError(),
                scheduled.getCreatedAt()
        );
    }

    public static WorkbenchKolDto toWorkbenchKolDto(
            KolDO kol,
            String ownerName,
            EmailDO latestEmail,
            int unreadCount,
            boolean unreplied,
            boolean awaitingReply
    ) {
        KolDto base = toKolDto(kol);
        if (base == null) {
            return null;
        }
        return new WorkbenchKolDto(
                base.id(),
                base.email(),
                base.name(),
                base.handle(),
                base.primaryPlatform(),
                base.type(),
                base.externalProfileUrl(),
                base.source(),
                base.feishuRecordId(),
                base.feishuOutreachAt(),
                base.stage(),
                base.status(),
                base.ownerUserId(),
                base.lastInboundAt(),
                base.lastOutboundAt(),
                base.agreedPrice(),
                base.brandQuote(),
                base.finalCooperationPrice(),
                base.agreedDeadline(),
                base.notes(),
                base.replyResolved(),
                base.stageOverride(),
                base.createdAt(),
                base.updatedAt(),
                ownerName,
                toEmailDto(latestEmail),
                unreadCount,
                unreplied,
                awaitingReply
        );
    }

    private static String enumToJson(Platform platform) {
        return platform == null ? null : platform.dbValue();
    }

    private static String enumToJson(KolStage stage) {
        return stage == null ? null : stage.name().toLowerCase();
    }

    private static String enumToJson(KolStatus status) {
        return status == null ? null : status.dbValue();
    }

    private static String enumToJson(EmailDirection direction) {
        return direction == null ? null : direction.name().toLowerCase();
    }

    private static String roleToJson(String role) {
        return role == null ? null : UserRole.fromDbValue(role).dbValue();
    }

    private static String statusToJson(String status) {
        return status == null ? null : UserStatus.fromDbValue(status).dbValue();
    }

    private static OffsetDateTime localDateToOffsetDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private static List<String> copyList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
