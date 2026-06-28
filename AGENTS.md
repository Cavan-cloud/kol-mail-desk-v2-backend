# Kol Mail Desk v2 — Backend (Spring Boot + Spring AI)

## Purpose

Java-side rewrite of kol-mail-desk. This repo OWNS:

- Auth, OAuth2 (Google login + Gmail authorization)
- Domain & data: KOL, Email, Template, ScheduledEmail, Audit
- Integrations: Gmail (sync/send), Feishu (read-only sheet sync)
- AI: Spring AI (classify, draft, check, translate) against Kimi/Moonshot
- Worker: scheduled email dispatch, Gmail sync jobs

## Stack

- Java 21 + Spring Boot 3.3
- Spring Security 6 + OAuth2
- Spring AI (OpenAI-compatible client)
- PostgreSQL 16 + Flyway
- Redis (cache + distributed lock)

## Module structure (planned)

```
maildesk-common · maildesk-domain · maildesk-infrastructure
maildesk-integration{gmail,feishu} · maildesk-ai
maildesk-application · maildesk-api · maildesk-worker
```

## Out of scope (in this repo)

- All frontend code (lives in kol-mail-desk-v2-web)
- UI strings (frontend owns Chinese copy)
- Vercel-style serverless. We run as standalone Spring services.

## Source of truth

跨仓库规格（架构、Phase、Feature Parity、决策、契约）：

- Refactor plan:    ../kol-mail-desk-v2-docs/specs/00-refactor-plan.md
- Architecture:     ../kol-mail-desk-v2-docs/specs/01-architecture.md
- Backend design:   ../kol-mail-desk-v2-docs/specs/02-backend-design.md
- Phases:           ../kol-mail-desk-v2-docs/specs/04-phases.md
- Feature parity:   ../kol-mail-desk-v2-docs/specs/05-feature-parity.md
- Testing:          ../kol-mail-desk-v2-docs/specs/06-testing.md
- Risks:            ../kol-mail-desk-v2-docs/specs/07-risks.md
- ADRs:             ../kol-mail-desk-v2-docs/specs/decisions/ADR-*.md
- API contract:     ../kol-mail-desk-v2-docs/specs/api-contract-v1.yaml

本仓库内规则（写代码必读）：

- 工程结构: docs/standards/project-structure.md
- 编码规范: docs/standards/coding-standards.md
- 开发流程: docs/standards/dev-workflow.md
- Agent 核心规则: .cursor/rules/backend-java.mdc（重命名 `cursor-rules-staging/` 后生效）

## Current phase

**Phase 0 — bootstrap.** Nothing implemented yet.

## Hard rules

- Do NOT read or modify the legacy repo at `/Users/chenkaifeng/code/kol-mail-desk`. Reference only.
- API endpoints MUST be defined in `api-contract-v1.yaml` BEFORE controllers are written.
- Every schema change MUST go through Flyway migration (no auto-DDL in production profiles).
- Feishu integration is READ-ONLY. Never call write APIs.
- AI is advisory. Never auto-send emails from AI output.
- Multi-tenancy: every business table MUST have `tenant_id` even if v1 only ships single-tenant.
- OAuth Token MUST be AES-256 encrypted at rest. Never log tokens.
- Every write operation MUST emit an audit log row to `actions`.
- Follow the 10-step workflow in `docs/standards/dev-workflow.md` (requirement → analysis → design → tasks → impl → review → tests → run tests → docs update → commit).

## Definition of done (per change)

- `mvn -pl <module> -am verify` passes for affected modules.
- ArchUnit tests pass (module dependency direction respected).
- New / changed endpoints reflected in `kol-mail-desk-v2-docs/specs/api-contract-v1.yaml`.
- Feishu / Gmail / AI access goes through `maildesk-domain` interfaces (testable without external calls).
- Schema changes only via Flyway `V{n}__{desc}.sql`.
- Multi-tenant filtering verified (`tenant_id` in queries, write paths inject from `TenantContext`).
- Audit log written for write operations.
- Long-form docs updated under `kol-mail-desk-v2-docs/specs/` when business rules / contracts / architecture changed.
- Conventional Commits (`feat(workbench): ...`, `fix(gmail-sync): ...`).
