# syntax=docker/dockerfile:1
#
# Multi-stage build for maildesk-api or maildesk-worker.
#
#   docker build --build-arg BUILD_MODULE=maildesk-api -t maildesk-api:local .
#   docker build --build-arg BUILD_MODULE=maildesk-worker --build-arg HEALTH_CHECK_PORT=8081 -t maildesk-worker:local .
#
# BUILD_MODULE must be one of: maildesk-api | maildesk-worker

ARG BUILD_MODULE=maildesk-api
ARG HEALTH_CHECK_PORT=8080

# ---- Dependencies (layer cache) ----
FROM maven:3.9-eclipse-temurin-21-alpine AS deps

WORKDIR /build

COPY pom.xml .
COPY maildesk-common/pom.xml maildesk-common/pom.xml
COPY maildesk-domain/pom.xml maildesk-domain/pom.xml
COPY maildesk-infrastructure/pom.xml maildesk-infrastructure/pom.xml
COPY maildesk-integration/pom.xml maildesk-integration/pom.xml
COPY maildesk-ai/pom.xml maildesk-ai/pom.xml
COPY maildesk-application/pom.xml maildesk-application/pom.xml
COPY maildesk-api/pom.xml maildesk-api/pom.xml
COPY maildesk-worker/pom.xml maildesk-worker/pom.xml

ARG BUILD_MODULE
RUN mvn -B -ntp dependency:go-offline -pl "${BUILD_MODULE}" -am \
    || mvn -B -ntp dependency:resolve -pl "${BUILD_MODULE}" -am

# ---- Compile + repackage ----
FROM deps AS builder

ARG BUILD_MODULE

COPY maildesk-common/src maildesk-common/src
COPY maildesk-domain/src maildesk-domain/src
COPY maildesk-infrastructure/src maildesk-infrastructure/src
COPY maildesk-integration/src maildesk-integration/src
COPY maildesk-ai/src maildesk-ai/src
COPY maildesk-application/src maildesk-application/src
COPY "${BUILD_MODULE}/src" "${BUILD_MODULE}/src"

RUN mvn -B -ntp -DskipTests package -pl "${BUILD_MODULE}" -am \
    && cp "${BUILD_MODULE}/target/${BUILD_MODULE}-"*.jar /build/app.jar

# ---- Runtime ----
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk add --no-cache curl \
    && addgroup -g 1001 maildesk \
    && adduser -u 1001 -G maildesk -D maildesk

WORKDIR /app

COPY --from=builder --chown=maildesk:maildesk /build/app.jar app.jar

USER maildesk

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ARG HEALTH_CHECK_PORT=8080
EXPOSE ${HEALTH_CHECK_PORT}

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${HEALTH_CHECK_PORT}/actuator/health" | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
