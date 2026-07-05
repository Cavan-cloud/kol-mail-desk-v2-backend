package com.lovart.maildesk.domain.profile.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import com.lovart.maildesk.common.util.Uuids;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Application user profile. {@code tenant_id} is auto-injected by
 * {@code TenantLineInnerInterceptor}. {@code google_sub} is the stable Google
 * subject identifier (V14 migration) and is the upsert key on login.
 */
@TableName("profiles")
public class ProfileDO {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private UUID tenantId;

    @TableField("display_name")
    private String displayName;

    private String email;

    private String role;

    private String status;

    @TableField("mentor_user_id")
    private UUID mentorUserId;

    @TableField("feishu_operator_name")
    private String feishuOperatorName;

    @TableField("last_synced_history_id")
    private String lastSyncedHistoryId;

    @TableField("last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @TableField("approved_at")
    private OffsetDateTime approvedAt;

    @TableField("approved_by")
    private UUID approvedBy;

    @TableField("departed_at")
    private OffsetDateTime departedAt;

    @TableField("google_sub")
    private String googleSub;

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
    public void setId(Object id) { this.id = Uuids.parse(id); }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getMentorUserId() { return mentorUserId; }
    public void setMentorUserId(UUID mentorUserId) { this.mentorUserId = mentorUserId; }

    public String getFeishuOperatorName() { return feishuOperatorName; }
    public void setFeishuOperatorName(String feishuOperatorName) { this.feishuOperatorName = feishuOperatorName; }

    public String getLastSyncedHistoryId() { return lastSyncedHistoryId; }
    public void setLastSyncedHistoryId(String lastSyncedHistoryId) { this.lastSyncedHistoryId = lastSyncedHistoryId; }

    public OffsetDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(OffsetDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }

    public UUID getApprovedBy() { return approvedBy; }
    public void setApprovedBy(UUID approvedBy) { this.approvedBy = approvedBy; }

    public OffsetDateTime getDepartedAt() { return departedAt; }
    public void setDepartedAt(OffsetDateTime departedAt) { this.departedAt = departedAt; }

    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }

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
        if (!(o instanceof ProfileDO that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
