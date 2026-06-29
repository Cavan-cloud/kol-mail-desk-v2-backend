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

See `../kol-mail-desk-v2-docs/specs/` for architecture and phase plan.
