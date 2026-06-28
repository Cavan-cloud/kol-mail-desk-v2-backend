# maildesk-backend 项目规则

本目录是 maildesk 后端的开发规则集，所有开发者和 Agent 必须遵守。

## 文档列表

| 文档 | 内容 |
|------|------|
| `project-structure.md` | Maven 多模块工程结构、目录约定、依赖方向 |
| `coding-standards.md` | Java 21 编码规范、分层规范、MyBatis-Plus 约定 |
| `dev-workflow.md` | 需求 → 分析 → 方案 → 任务 → 实现 → Review → 测试 → 提交流程 |

## 与全局规格的关系

- 本目录是**后端专属**规则
- 全局架构、决策、Phase 计划在 `../../../kol-mail-desk-v2-docs/specs/`
- 关键摘要在 `../../.cursor/rules/backend-java.mdc`（重命名 `cursor-rules-staging` 后生效），Agent 任务时自动加载

## 来源

这套规则原型来自另一项目（东方财富私募产品 `com.eastmoney.privateproduct`）的：

- `项目编码规范.md`
- `开发流程规范.md`
- `工程结构.md`

已适配本项目栈差异：

| 原项目 | maildesk-v2 |
|--------|-------------|
| `com.eastmoney.privateproduct` | `com.lovart.maildesk` |
| 单 Maven module | 8 个模块 |
| MyBatis + 双数据源（MySQL + Choice/Oracle） | **MyBatis-Plus + 单 PostgreSQL**（详见 [ADR-006](../../../kol-mail-desk-v2-docs/specs/decisions/ADR-006-orm-mybatis-plus.md)） |
| Apollo | application.yml + Secrets Manager |
| OSS | S3 |
| XXL-JOB | Spring Scheduler + Redis Lock |
| `.harness/rules/`、`.harness/wiki/` | `.cursor/rules/`、`docs/specs/`、`docs/standards/` |

并新增本项目特有约束：

- 多租户：所有业务表必须有 `tenant_id`
- 集成边界：Gmail / 飞书 / AI 走 domain 接口
- 飞书只读、AI 永不自动发信
- OAuth Token 加密存储 + 审计日志
- OpenAPI 契约同步

## 文档更新

- 规则变更通过 PR 走 Review
- 关键变更同步到 `.cursor/rules/backend-java.mdc`
- 历史规则在 Git 历史中追溯
