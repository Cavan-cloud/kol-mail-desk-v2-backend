package com.lovart.maildesk.domain.email.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.typehandler.EmailDirectionTypeHandler;
import com.lovart.maildesk.common.typehandler.JsonbTypeHandler;
import com.lovart.maildesk.common.typehandler.KolStageTypeHandler;
import com.lovart.maildesk.common.typehandler.StringArrayTypeHandler;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Minimal mailbox record used to exercise:
 * <ul>
 *   <li>{@link StringArrayTypeHandler} on the {@code text[]} columns
 *       ({@code to_emails} / {@code cc_emails}),</li>
 *   <li>{@link JsonbTypeHandler} on the {@code jsonb} column
 *       ({@code ai_extracted_fields}),</li>
 *   <li>{@link EmailDirectionTypeHandler} on the {@code email_direction} ENUM.</li>
 * </ul>
 *
 * {@code @TableName(autoResultMap = true)} is mandatory: it forces MyBatis-Plus
 * to emit a generated {@code <resultMap>} that honours the per-field
 * {@code typeHandler} hints during SELECT mapping.
 */
@TableName(value = "emails", autoResultMap = true)
public class EmailDO {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private UUID tenantId;

    @TableField("gmail_message_id")
    private String gmailMessageId;

    @TableField("gmail_thread_id")
    private String gmailThreadId;

    @TableField("kol_id")
    private UUID kolId;

    @TableField("user_id")
    private UUID userId;

    @TableField("template_id")
    private UUID templateId;

    @TableField(value = "direction", typeHandler = EmailDirectionTypeHandler.class)
    private EmailDirection direction;

    @TableField("from_email")
    private String fromEmail;

    @TableField(value = "to_emails", typeHandler = StringArrayTypeHandler.class)
    private List<String> toEmails;

    @TableField(value = "cc_emails", typeHandler = StringArrayTypeHandler.class)
    private List<String> ccEmails;

    private String subject;

    @TableField("body_text")
    private String bodyText;

    @TableField("body_html")
    private String bodyHtml;

    @TableField("body_zh")
    private String bodyZh;

    @TableField(value = "attachment_names", typeHandler = StringArrayTypeHandler.class)
    private List<String> attachmentNames;

    @TableField("has_attachments")
    private Boolean hasAttachments;

    @TableField(value = "ai_stage_signal", typeHandler = KolStageTypeHandler.class)
    private KolStage aiStageSignal;

    @TableField("ai_priority")
    private String aiPriority;

    @TableField("ai_summary")
    private String aiSummary;

    @TableField("ai_suggested_action")
    private String aiSuggestedAction;

    @TableField(value = "ai_extracted_fields", typeHandler = JsonbTypeHandler.class)
    private JsonNode aiExtractedFields;

    @TableField("is_read")
    private Boolean isRead;

    @TableField("read_at")
    private OffsetDateTime readAt;

    @TableField("sent_at")
    private OffsetDateTime sentAt;

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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getGmailMessageId() {
        return gmailMessageId;
    }

    public void setGmailMessageId(String gmailMessageId) {
        this.gmailMessageId = gmailMessageId;
    }

    public String getGmailThreadId() {
        return gmailThreadId;
    }

    public void setGmailThreadId(String gmailThreadId) {
        this.gmailThreadId = gmailThreadId;
    }

    public UUID getKolId() {
        return kolId;
    }

    public void setKolId(UUID kolId) {
        this.kolId = kolId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public EmailDirection getDirection() {
        return direction;
    }

    public void setDirection(EmailDirection direction) {
        this.direction = direction;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public List<String> getToEmails() {
        return toEmails;
    }

    public void setToEmails(List<String> toEmails) {
        this.toEmails = toEmails;
    }

    public List<String> getCcEmails() {
        return ccEmails;
    }

    public void setCcEmails(List<String> ccEmails) {
        this.ccEmails = ccEmails;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public String getBodyZh() {
        return bodyZh;
    }

    public void setBodyZh(String bodyZh) {
        this.bodyZh = bodyZh;
    }

    public List<String> getAttachmentNames() {
        return attachmentNames;
    }

    public void setAttachmentNames(List<String> attachmentNames) {
        this.attachmentNames = attachmentNames;
    }

    public Boolean getHasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(Boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }

    public KolStage getAiStageSignal() {
        return aiStageSignal;
    }

    public void setAiStageSignal(KolStage aiStageSignal) {
        this.aiStageSignal = aiStageSignal;
    }

    public String getAiPriority() {
        return aiPriority;
    }

    public void setAiPriority(String aiPriority) {
        this.aiPriority = aiPriority;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getAiSuggestedAction() {
        return aiSuggestedAction;
    }

    public void setAiSuggestedAction(String aiSuggestedAction) {
        this.aiSuggestedAction = aiSuggestedAction;
    }

    public JsonNode getAiExtractedFields() {
        return aiExtractedFields;
    }

    public void setAiExtractedFields(JsonNode aiExtractedFields) {
        this.aiExtractedFields = aiExtractedFields;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(OffsetDateTime readAt) {
        this.readAt = readAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailDO that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
