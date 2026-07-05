# Kubernetes 部署

生产环境以 **Helm chart** 为唯一维护源：`deploy/helm/maildesk/`。

## 前置

- 托管 **PostgreSQL 16** + **Redis 7**（或集群内 Operator，自行改 `values.yaml` 中的 host）
- 容器镜像来自 P6-T06 Dockerfile（CI 构建后推送到你的 registry）
- 密钥通过 K8s Secret / External Secrets 注入 — 完整方案见 [`deploy/secrets/README.md`](../secrets/README.md)（P6-T13）

## 安装

```bash
# 校验 chart
helm lint deploy/helm/maildesk \
  --set database.host=postgres.example.svc \
  --set redis.host=redis.example.svc \
  --set config.corsAllowedOrigins=https://app.example.com \
  --set config.webRedirectUrl=https://app.example.com/

# 渲染 manifest（可用于 GitOps / 审计，勿手改输出）
helm template maildesk deploy/helm/maildesk \
  --namespace maildesk \
  --set database.host=postgres.example.svc \
  --set redis.host=redis.example.svc \
  --set config.corsAllowedOrigins=https://app.example.com \
  --set config.webRedirectUrl=https://app.example.com/ \
  > deploy/k8s/rendered/maildesk.yaml

# 安装 / 升级
helm upgrade --install maildesk deploy/helm/maildesk \
  --namespace maildesk --create-namespace \
  -f deploy/helm/maildesk/values.yaml \
  --set global.imageRegistry=ghcr.io/your-org \
  --set api.image.tag=0.1.0 \
  --set worker.image.tag=0.1.0 \
  --set database.host=postgres.example.svc \
  --set redis.host=redis.example.svc \
  --set config.corsAllowedOrigins=https://app.example.com \
  --set config.webRedirectUrl=https://app.example.com/ \
  --set ingress.enabled=true \
  --set ingress.hosts[0].host=api.example.com
```

## 组件

| 资源 | 说明 |
|------|------|
| `Deployment/maildesk-api` | REST + OAuth2，默认 2 副本，可选 HPA |
| `Deployment/maildesk-worker` | 定时同步 / 定时发信，**默认 1 副本**（见 R7） |
| `Service` | API `:8080`，Worker management `:8081` |
| `Ingress` | 可选，仅暴露 API |
| `Secret` | 默认不创建；`externalSecrets.enabled=true` 经 ESO 从云 SM 同步；`secrets.create=true` 仅供本地 smoke |

## 生产密钥（P6-T13）

见 [`deploy/secrets/README.md`](../secrets/README.md) · [`values-prod.example.yaml`](../helm/maildesk/values-prod.example.yaml)。

## 探针

- API：`GET /actuator/health:8080`
- Worker：`GET /actuator/health:8081`（`management.server.port`）

## 告警（P6-T09）

Prometheus 规则：`deploy/prometheus/alerts/maildesk.rules.yml`

Helm 启用：`--set alerts.enabled=true`（需集群安装 Prometheus Operator）

| 告警 | 条件 |
|------|------|
| GmailSyncFailuresElevated | 15m 内失败 ≥ 3 |
| AiFailureRateHigh | 5m 失败率 > 10% 持续 10m |
| ScheduledEmailDispatchLagHigh | lag > 300s 持续 5m |

## 数据迁移（P6-T10）

见 [`kol-mail-desk-v2-docs/scripts/migration/README.md`](../../../kol-mail-desk-v2-docs/scripts/migration/README.md)。

## 双跑 / 切流 / 回滚（P6-T11 / P6-T12）

见 [`kol-mail-desk-v2-docs/scripts/cutover/README.md`](../../../kol-mail-desk-v2-docs/scripts/cutover/README.md) · [`cutover-runbook.md`](../../../kol-mail-desk-v2-docs/scripts/cutover/cutover-runbook.md) · [`rollback-runbook.md`](../../../kol-mail-desk-v2-docs/scripts/cutover/rollback-runbook.md)。

## 目录

```
deploy/
  helm/maildesk/     ← 维护此 chart
  k8s/
    README.md        ← 本文件
    rendered/        ← helm template 输出（gitignore，本地生成）
```
