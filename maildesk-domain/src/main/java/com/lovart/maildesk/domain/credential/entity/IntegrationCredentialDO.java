package com.lovart.maildesk.domain.credential.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Encrypted external integration credential.
 * <p>
 * {@code encryptedPayload} holds AES-256-GCM ciphertext of a small JSON document
 * (access_token / refresh_token / scope / expires_at / token_type). Tokens are
 * NEVER persisted plaintext and NEVER logged.
 */
@TableName("integration_credentials")
public class IntegrationCredentialDO {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private UUID tenantId;

    @TableField("user_id")
    private UUID userId;

    private String type;

    @TableField("encrypted_payload")
    private byte[] encryptedPayload;

    @TableField("expires_at")
    private OffsetDateTime expiresAt;

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

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public byte[] getEncryptedPayload() { return encryptedPayload; }
    public void setEncryptedPayload(byte[] encryptedPayload) { this.encryptedPayload = encryptedPayload; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

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
        if (!(o instanceof IntegrationCredentialDO that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
