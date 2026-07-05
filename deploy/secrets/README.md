# 生产密钥管理（P6-T13）

> **原则**：密钥只存在于云 Secrets Manager / Vault，经 **External Secrets Operator (ESO)** 同步到 K8s `Secret`；**禁止**把真实值写入 Helm `values.yaml` 或 git（`07-risks.md` R12）。

---

## 架构

```
AWS Secrets Manager / GCP Secret Manager
        │  (JSON: maildesk/prod)
        ▼
ExternalSecret (Helm)  ──refresh──►  K8s Secret: maildesk-secrets
        ▲
SecretStore + IRSA / Workload Identity
        │
Deployment (API / Worker)  envFrom secretKeyRef
```

| 层 | 职责 |
|----|------|
| **云 SM** | 权威存储、轮换、审计 |
| **ESO** | 按 `refreshInterval` 拉取 → K8s Secret |
| **Helm** | `secrets.create=false` · `externalSecrets.enabled=true` |
| **Pod** | `maildesk.secretEnv` 注入环境变量 |

---

## 密钥清单

云 SM 中建议使用 **单个 JSON Secret**（名称 `maildesk/prod`），字段与 `.env.example` 一致：

| JSON 字段 | K8s Secret key | 必填 | 说明 |
|-----------|----------------|------|------|
| `TOKEN_ENCRYPTION_KEY` | `token-encryption-key` | ✅ | `openssl rand -base64 32` |
| `DB_PASSWORD` | `db-password` | ✅ | PG 密码 |
| `REDIS_PASSWORD` | `redis-password` | | 空字符串可 |
| `GOOGLE_OAUTH_CLIENT_ID` | `google-oauth-client-id` | ✅ | GCP OAuth |
| `GOOGLE_OAUTH_CLIENT_SECRET` | `google-oauth-client-secret` | ✅ | |
| `FEISHU_APP_ID` | `feishu-app-id` | | 飞书同步 |
| `FEISHU_APP_SECRET` | `feishu-app-secret` | | |
| `FEISHU_KOL_APP_TOKEN` | `feishu-kol-app-token` | | |
| `FEISHU_KOL_TABLE_ID` | `feishu-kol-table-id` | | |
| `MOONSHOT_API_KEY` | `moonshot-api-key` | ⚠️ | AI 至少填一个 provider |
| `DEEPSEEK_API_KEY` | `deepseek-api-key` | ⚠️ | |

模板文件（**勿提交真实值**）：[`maildesk-prod.secret.json.example`](./maildesk-prod.secret.json.example)

---

## 1. 安装 External Secrets Operator

```bash
helm repo add external-secrets https://charts.external-secrets.io
helm upgrade --install external-secrets external-secrets/external-secrets \
  -n external-secrets --create-namespace
```

---

## 2. 写入云 Secrets Manager

### AWS

```bash
# 从模板编辑（不要 commit 填好后的文件）
cp maildesk-prod.secret.json.example /tmp/maildesk-prod.json
# 编辑 /tmp/maildesk-prod.json

aws secretsmanager create-secret \
  --name maildesk/prod \
  --secret-string file:///tmp/maildesk-prod.json \
  --region ap-southeast-1

# 更新已有 secret
aws secretsmanager put-secret-value \
  --secret-id maildesk/prod \
  --secret-string file:///tmp/maildesk-prod.json \
  --region ap-southeast-1

rm /tmp/maildesk-prod.json
```

### GCP Secret Manager

```bash
gcloud secrets create maildesk-prod --replication-policy=automatic
gcloud secrets versions add maildesk-prod --data-file=/tmp/maildesk-prod.json
```

Helm `values-prod.example.yaml` 中把 `remoteRef.secretName` 改为 GCP secret 名称，provider 改为 `gcpsm`。

---

## 3. IAM / Workload Identity

### AWS EKS（IRSA）

1. 创建 IAM Policy（`secretsmanager:GetSecretValue` on `maildesk/prod`）
2. 绑定 Role 到 K8s ServiceAccount（与 Helm release 同名 namespace `maildesk`）
3. 在 `values-prod.example.yaml` 设置：

```yaml
serviceAccount:
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT:role/maildesk-external-secrets
```

### GCP GKE

在 `values.yaml` / prod override 中配置：

```yaml
externalSecrets:
  secretStore:
    provider: gcpsm
    gcpsm:
      projectID: your-project
      clusterLocation: asia-southeast1
      clusterName: your-cluster
```

并为 ServiceAccount 绑定 `roles/secretmanager.secretAccessor`。

---

## 4. Helm 部署（生产）

```bash
helm upgrade --install maildesk deploy/helm/maildesk \
  -n maildesk --create-namespace \
  -f deploy/helm/maildesk/values-prod.example.yaml \
  --set database.host=YOUR_PG_HOST \
  --set redis.host=YOUR_REDIS_HOST \
  --set config.corsAllowedOrigins=https://app.yourdomain.com \
  --set config.webRedirectUrl=https://app.yourdomain.com/
```

**禁止** `--set secrets.tokenEncryptionKey=...` 或把密钥写进 `-f` 文件并 commit。

---

## 5. 验证

```bash
# ESO 同步状态
kubectl get externalsecret,secretstore -n maildesk

# Secret 键齐全
chmod +x deploy/secrets/verify-k8s-secret.sh
NAMESPACE=maildesk ./deploy/secrets/verify-k8s-secret.sh

# Pod 已挂载（不应在 describe 里看到明文值）
kubectl get pods -n maildesk
kubectl exec -n maildesk deploy/maildesk-api -- printenv | grep -E '^(TOKEN|GOOGLE|MOONSHOT)=' | sed 's/=.*$/=***/'
```

---

## 6. 密钥轮换

| 密钥 | 轮换方式 | 应用重启 |
|------|----------|----------|
| `DB_PASSWORD` | PG `ALTER USER` + SM 更新 | 是（或等 ESO refresh + pod reload） |
| `TOKEN_ENCRYPTION_KEY` | **极高风险** — 需 re-encrypt `integration_credentials` | 是 |
| OAuth / AI / 飞书 | SM 更新 → ESO 自动 refresh（默认 1h） | 可选滚动 restart |

轮换后：

```bash
kubectl rollout restart deployment/maildesk-api deployment/maildesk-worker -n maildesk
```

---

## 7. 本地 / minikube

本地开发继续用 `.env` + `docker-compose.dev.yml`，**不要**把 `.env` 打进镜像。

Minikube smoke 可用 Helm 内联 Secret（**仅本地**）：

```bash
helm upgrade --install maildesk deploy/helm/maildesk \
  -f deploy/helm/maildesk/values-local.example.yaml
```

`values-local.example.yaml` 中 `secrets.create=true` — **禁止**用于生产。

---

## 8. CI 守护

`deploy/scripts/guard-no-plaintext-secrets.sh` 在 CI 中检查：

- `values.yaml` 保持 `secrets.create=false`
- `deploy/` 下无 `sk-` / OAuth token 泄漏模式

---

## 引用

- Helm chart：`deploy/helm/maildesk/`
- 生产 values 示例：`deploy/helm/maildesk/values-prod.example.yaml`
- 切流 Runbook：`kol-mail-desk-v2-docs/scripts/cutover/cutover-runbook.md`
- 风险：`kol-mail-desk-v2-docs/specs/07-risks.md` R12
