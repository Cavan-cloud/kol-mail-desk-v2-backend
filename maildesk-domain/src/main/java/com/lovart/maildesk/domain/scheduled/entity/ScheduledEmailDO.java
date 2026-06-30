package com.lovart.maildesk.domain.scheduled.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lovart.maildesk.common.typehandler.StringArrayTypeHandler;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@TableName(value = "scheduled_emails", autoResultMap = true)
public class ScheduledEmailDO {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private UUID tenantId;

    @TableField("kol_id")
    private UUID kolId;

    @TableField("user_id")
    private UUID userId;

    @TableField("template_id")
    private UUID templateId;

    @TableField("to_email")
    private String toEmail;

    @TableField(value = "cc_emails", typeHandler = StringArrayTypeHandler.class)
    private List<String> ccEmails;

    private String subject;

    @TableField("english_body")
    private String englishBody;

    @TableField("english_body_html")
    private String englishBodyHtml;

    @TableField("chinese_draft")
    private String chineseDraft;

    @TableField("scheduled_at")
    private OffsetDateTime scheduledAt;

    private String status;

    @TableField("attempt_count")
    private Integer attemptCount;

    @TableField("last_attempt_at")
    private OffsetDateTime lastAttemptAt;

    @TableField("gmail_message_id")
    private String gmailMessageId;

    private String error;

    @TableField("sent_at")
    private OffsetDateTime sentAt;

    @TableField("cancelled_at")
    private OffsetDateTime cancelledAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(value = "created_by", fill = FieldFill.INSERT,
            updateStrategy = FieldStrategy.NEVER)
    private UUID createdBy;

    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private UUID updatedBy;

    @TableLogic(value = "null", delval = "now()")
    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @Version
    private Integer version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getKolId() { return kolId; }
    public void setKolId(UUID kolId) { this.kolId = kolId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public String getToEmail() { return toEmail; }
    public void setToEmail(String toEmail) { this.toEmail = toEmail; }

    public List<String> getCcEmails() { return ccEmails; }
    public void setCcEmails(List<String> ccEmails) { this.ccEmails = ccEmails; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getEnglishBody() { return englishBody; }
    public void setEnglishBody(String englishBody) { this.englishBody = englishBody; }

    public String getEnglishBodyHtml() { return englishBodyHtml; }
    public void setEnglishBodyHtml(String englishBodyHtml) { this.englishBodyHtml = englishBodyHtml; }

    public String getChineseDraft() { return chineseDraft; }
    public void setChineseDraft(String chineseDraft) { this.chineseDraft = chineseDraft; }

    public OffsetDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public OffsetDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(OffsetDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public String getGmailMessageId() { return gmailMessageId; }
    public void setGmailMessageId(String gmailMessageId) { this.gmailMessageId = gmailMessageId; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public OffsetDateTime getSentAt() { return sentAt; }
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }

    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduledEmailDO that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
