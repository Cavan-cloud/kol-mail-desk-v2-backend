# kol-mail-desk-v2-backend

[![backend-ci](https://github.com/ORG/kol-mail-desk-v2-backend/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/ORG/kol-mail-desk-v2-backend/actions/workflows/backend-ci.yml)

Spring Boot + Spring AI rewrite of kol-mail-desk.

**Status: Phase 1 — Maven 多模块骨架已落地（P1-T01 ✅）。**

## 本地构建

- **Java 21 JDK**（Temurin / Homebrew `openjdk@21`）
- **Maven 3.9+**

```bash
# Homebrew 安装后，必须指向 JDK 21（不要用系统自带的 Java 8 JRE）
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

java -version   # 应显示 openjdk 21
javac -version  # 必须有 javac，否则 Maven 报 "No compiler is provided"

cd kol-mail-desk-v2-backend
mvn -B verify
```

建议把 `JAVA_HOME` 和 `PATH` 写入 `~/.zprofile`，避免每次手动 export。

```bash
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"' >> ~/.zprofile
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zprofile
```

可选：`docker compose -f docker-compose.dev.yml up -d`（PG + Redis，P1-T02 起需要）。

## Docker 镜像（P6-T06）

多阶段 `Dockerfile`，按模块构建 **API** 或 **Worker** 可执行 JAR 镜像（Temurin 21 JRE Alpine，非 root 运行）：

```bash
# 仅构建镜像
docker build --build-arg BUILD_MODULE=maildesk-api -t maildesk-api:local .
docker build --build-arg BUILD_MODULE=maildesk-worker --build-arg HEALTH_CHECK_PORT=8081 -t maildesk-worker:local .

# 本地全栈（PG + Redis + API + Worker）
cp .env.example .env   # 填入 TOKEN_ENCRYPTION_KEY 等
docker compose -f docker-compose.dev.yml -f docker-compose.app.yml up -d --build
```

- API 健康检查：`http://localhost:8080/actuator/health`
- Worker 健康检查：`http://localhost:8081/actuator/health`

生产部署走 K8s / Helm（P6-T07），本仓库 `docker-compose.app.yml` 仅供本地验证镜像。

## Kubernetes / Helm（P6-T07）

Chart 位于 `deploy/helm/maildesk/`（API + Worker 双 Deployment、Service、可选 Ingress/HPA）。

```bash
helm lint deploy/helm/maildesk \
  --set database.host=postgres.example.svc \
  --set redis.host=redis.example.svc \
  --set config.corsAllowedOrigins=https://app.example.com \
  --set config.webRedirectUrl=https://app.example.com/

helm upgrade --install maildesk deploy/helm/maildesk \
  --namespace maildesk --create-namespace \
  --set database.host=... --set redis.host=... \
  --set config.corsAllowedOrigins=... --set config.webRedirectUrl=...
```

详见 [`deploy/k8s/README.md`](deploy/k8s/README.md)。

## 可观测性（P6-T08）

Micrometer + Prometheus（`/actuator/prometheus`）+ OpenTelemetry OTLP tracing + Grafana dashboard。

```bash
# 本地监控栈（需先起 app compose）
docker compose -f docker-compose.dev.yml -f docker-compose.app.yml -f docker-compose.observability.yml up -d
# Grafana http://localhost:3001  Prometheus http://localhost:9090
```

指标：`gmail.sync.duration` · `gmail.sync.failed` · `ai.invocation` · `ai.classify.tokens` · `scheduled_email.dispatch.lag_seconds`

Dashboard：`deploy/grafana/dashboards/maildesk-overview.json`

告警规则：`deploy/prometheus/alerts/maildesk.rules.yml`（Helm：`alerts.enabled=true`）

## 数据迁移（P6-T10）

脚本：`../kol-mail-desk-v2-docs/scripts/migration/`（`migrate.sh` · `diff.sh` · Google token 加密迁移）

See `../kol-mail-desk-v2-docs/specs/` for architecture and phase plan.
