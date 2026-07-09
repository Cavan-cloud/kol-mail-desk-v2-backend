package com.lovart.maildesk.domain.kol.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.enums.KolStatus;
import com.lovart.maildesk.common.enums.Platform;
import com.lovart.maildesk.common.util.Uuids;
import com.lovart.maildesk.common.typehandler.KolStageTypeHandler;
import com.lovart.maildesk.common.typehandler.KolStatusTypeHandler;
import com.lovart.maildesk.common.typehandler.PlatformTypeHandler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * KOL row. {@code tenant_id} is automatically injected by
 * {@code TenantLineInnerInterceptor} on writes and reads; callers MUST NOT set it
 * explicitly in service code. {@code normalized_email} is a PG generated column
 * and is omitted from this DO so MyBatis-Plus does not try to write to it.
 * <p>
 * {@code @TableName(autoResultMap = true)} is required because {@code stage} uses
 * a non-default {@link KolStageTypeHandler} — auto result map generation reuses
 * the per-field handler hint at SELECT time.
 */
@TableName(value = "kols", autoResultMap = true)
public class KolDO {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private UUID tenantId;

    private String email;

    private String name;

    private String handle;

    @TableField(value = "primary_platform", typeHandler = PlatformTypeHandler.class)
    private Platform primaryPlatform;

    @TableField(value = "stage", typeHandler = KolStageTypeHandler.class)
    private KolStage stage;

    @TableField(value = "status", typeHandler = KolStatusTypeHandler.class)
    private KolStatus status;

    @TableField("owner_user_id")
    private UUID ownerUserId;

    @TableField("last_inbound_at")
    private OffsetDateTime lastInboundAt;

    @TableField("last_outbound_at")
    private OffsetDateTime lastOutboundAt;

    @TableField("agreed_price")
    private BigDecimal agreedPrice;

    @TableField("brand_quote")
    private String brandQuote;

    @TableField("final_cooperation_price")
    private BigDecimal finalCooperationPrice;

    @TableField(value = "agreed_platform", typeHandler = PlatformTypeHandler.class)
    private Platform agreedPlatform;

    @TableField("agreed_deadline")
    private LocalDate agreedDeadline;

    private String notes;

    @TableField("type")
    private String type;

    @TableField("external_profile_url")
    private String externalProfileUrl;

    @TableField("platform_handle")
    private String platformHandle;

    private String source;

    @TableField("feishu_record_id")
    private String feishuRecordId;

    @TableField("feishu_table_id")
    private String feishuTableId;

    @TableField("feishu_operator_name")
    private String feishuOperatorName;

    @TableField("last_feishu_synced_at")
    private OffsetDateTime lastFeishuSyncedAt;

    @TableField("feishu_outreach_at")
    private LocalDate feishuOutreachAt;

    @TableField("reply_resolved")
    private Boolean replyResolved;

    @TableField("name_overridden")
    private Boolean nameOverridden;

    @TableField("stage_override")
    private Boolean stageOverride;

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

    public void setId(Object id) {
        this.id = Uuids.parse(id);
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public Platform getPrimaryPlatform() {
        return primaryPlatform;
    }

    public void setPrimaryPlatform(Platform primaryPlatform) {
        this.primaryPlatform = primaryPlatform;
    }

    public KolStage getStage() {
        return stage;
    }

    public void setStage(KolStage stage) {
        this.stage = stage;
    }

    public KolStatus getStatus() {
        return status;
    }

    public void setStatus(KolStatus status) {
        this.status = status;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public OffsetDateTime getLastInboundAt() {
        return lastInboundAt;
    }

    public void setLastInboundAt(OffsetDateTime lastInboundAt) {
        this.lastInboundAt = lastInboundAt;
    }

    public OffsetDateTime getLastOutboundAt() {
        return lastOutboundAt;
    }

    public void setLastOutboundAt(OffsetDateTime lastOutboundAt) {
        this.lastOutboundAt = lastOutboundAt;
    }

    public BigDecimal getAgreedPrice() {
        return agreedPrice;
    }

    public void setAgreedPrice(BigDecimal agreedPrice) {
        this.agreedPrice = agreedPrice;
    }

    public String getBrandQuote() {
        return brandQuote;
    }

    public void setBrandQuote(String brandQuote) {
        this.brandQuote = brandQuote;
    }

    public BigDecimal getFinalCooperationPrice() {
        return finalCooperationPrice;
    }

    public void setFinalCooperationPrice(BigDecimal finalCooperationPrice) {
        this.finalCooperationPrice = finalCooperationPrice;
    }

    public Platform getAgreedPlatform() {
        return agreedPlatform;
    }

    public void setAgreedPlatform(Platform agreedPlatform) {
        this.agreedPlatform = agreedPlatform;
    }

    public LocalDate getAgreedDeadline() {
        return agreedDeadline;
    }

    public void setAgreedDeadline(LocalDate agreedDeadline) {
        this.agreedDeadline = agreedDeadline;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExternalProfileUrl() {
        return externalProfileUrl;
    }

    public void setExternalProfileUrl(String externalProfileUrl) {
        this.externalProfileUrl = externalProfileUrl;
    }

    public String getPlatformHandle() {
        return platformHandle;
    }

    public void setPlatformHandle(String platformHandle) {
        this.platformHandle = platformHandle;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFeishuRecordId() {
        return feishuRecordId;
    }

    public void setFeishuRecordId(String feishuRecordId) {
        this.feishuRecordId = feishuRecordId;
    }

    public String getFeishuTableId() {
        return feishuTableId;
    }

    public void setFeishuTableId(String feishuTableId) {
        this.feishuTableId = feishuTableId;
    }

    public String getFeishuOperatorName() {
        return feishuOperatorName;
    }

    public void setFeishuOperatorName(String feishuOperatorName) {
        this.feishuOperatorName = feishuOperatorName;
    }

    public OffsetDateTime getLastFeishuSyncedAt() {
        return lastFeishuSyncedAt;
    }

    public void setLastFeishuSyncedAt(OffsetDateTime lastFeishuSyncedAt) {
        this.lastFeishuSyncedAt = lastFeishuSyncedAt;
    }

    public LocalDate getFeishuOutreachAt() {
        return feishuOutreachAt;
    }

    public void setFeishuOutreachAt(LocalDate feishuOutreachAt) {
        this.feishuOutreachAt = feishuOutreachAt;
    }

    public Boolean getReplyResolved() {
        return replyResolved;
    }

    public void setReplyResolved(Boolean replyResolved) {
        this.replyResolved = replyResolved;
    }

    public Boolean getNameOverridden() {
        return nameOverridden;
    }

    public void setNameOverridden(Boolean nameOverridden) {
        this.nameOverridden = nameOverridden;
    }

    public Boolean getStageOverride() {
        return stageOverride;
    }

    public void setStageOverride(Boolean stageOverride) {
        this.stageOverride = stageOverride;
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
        if (!(o instanceof KolDO that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
