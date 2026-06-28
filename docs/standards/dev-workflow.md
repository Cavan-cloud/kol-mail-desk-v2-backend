# 开发流程规范

约束开发者与 Agent 协作完成需求时的标准流程。除紧急修复或开发者明确要求跳过外，所有代码变更按本文档执行。

## 1. 输入需求

- 开发者输入需求内容
- 需求说明应包含：业务背景、目标功能、影响范围、验收标准、期望交付时间
- 涉及接口、页面、数据表、外部系统、权限规则时，同步提供相关文档、示例或现有代码位置
- 涉及密钥、账号、连接串等敏感信息时，通过环境变量或安全配置提供，**不写入对话、代码或文档**

## 2. 需求分析

- Agent 根据需求和已有代码分析
- 必须先搜索和阅读相关项目内容（重点是 `kol-mail-desk-v2-docs/specs/` 和本目录 `docs/standards/`），不得臆造接口、路径、配置或业务规则
- 必须核对 v3.3 功能对照清单（`kol-mail-desk-v2-docs/specs/05-feature-parity.md`）确认是否在重构范围
- Agent 输出需求分析，至少包括：
  - 需求目标
  - 当前项目相关现状
  - 影响范围（哪些模块、表、接口、外部系统）
  - 关键业务规则（含 v3.3 不可丢的体验）
  - 技术实现思路
  - 风险点和待确认问题
  - 初步验收标准
- Agent 输出后必须询问开发者意见
- 开发者认可后进入技术方案阶段；不认可则修改直到认可

## 3. 生成技术方案

- 基于已确认的需求分析生成技术方案
- 必须结合当前项目结构和既有实现，不能脱离项目现状另起架构
- 必须遵守 `project-structure.md`（多模块边界）和 `coding-standards.md`
- 内容至少包括：
  - 总体实现思路
  - 涉及模块和文件范围（按 8 个 Maven 模块定位）
  - Controller、Application Service、Domain、Mapper（接口 + XML）、Integration、AI、Worker、配置、Flyway 迁移、模板等改动点
  - 数据流转和模块依赖关系
  - 关键类、方法、SQL 的设计说明
  - 事务、权限、异常、日志、安全、**多租户、审计**、兼容性考虑
  - **OpenAPI 契约改动**（`kol-mail-desk-v2-docs/specs/api-contract-v1.yaml`）
  - 单元测试与验证策略
  - 风险点、替代方案、待确认事项
- 生成后询问开发者意见，认可后进入任务计划阶段

## 4. 生成开发任务计划

- 基于已确认的需求分析和技术方案生成
- 拆分为具体、可执行、可检查的任务
- 每个任务包含：
  - 任务目标
  - 涉及文件或模块
  - 主要修改点
  - 验收方式
- 明确任务顺序和依赖关系
- 不确定的任务先列为待确认事项，不直接进入编码
- 开发过程发现需求分析、技术方案、任务计划不准确，**暂停扩大改动**，先更新文档并向开发者说明

## 5. 开发实现

- Agent 按任务计划逐项完成
- 每次修改前说明将修改的文件和原因
- 实现必须遵守 `project-structure.md` 和 `coding-standards.md`
- 必须同步更新 `kol-mail-desk-v2-docs/specs/api-contract-v1.yaml`（如有 API 变更）
- 必须同步新增 Flyway 迁移（如有 schema 变更）
- 小步提交式开发，避免一次性大范围改动
- 不修改无关代码；不编辑 `target/`
- 新增配置、Flyway 迁移、模板、测试资源时说明用途和加载方式
- 遇到外部依赖、权限、环境阻塞时及时说明阻塞原因和可选处理方案

## 6. 代码 Review

- 所有开发任务完成后必须 Review
- 从缺陷风险角度检查：
  - 需求是否完整实现
  - 实现是否符合已确认的技术方案
  - 分层依赖是否符合：Controller → Application → Domain ← Integration / AI / Infrastructure
  - **ArchUnit 测试是否通过**
  - 参数校验、异常处理、统一响应是否完整
  - 事务边界是否合理（不在事务中跨远程调用）
  - SQL 是否安全（参数化、白名单、字段映射、分页）
  - **多租户字段（`tenant_id`）是否正确处理**
  - 集成层是否走接口（不绕过 domain）
  - **飞书是否保持只读**
  - **AI 是否保持「人工确认才发送」**
  - 日志是否足够定位问题且未输出敏感信息
  - **审计日志是否覆盖所有写操作**
  - 是否引入无关改动、硬编码、重复逻辑
  - 是否需要补充测试
- Agent 生成 Review 报告，包含：
  - 结论：通过 / 不通过
  - 发现的问题列表
  - 风险等级和影响
  - 建议修改方案
  - 已确认无需处理的原因
- Review 不通过则修复后重新 Review
- Review 通过进入单元测试阶段

## 7. 编写单元测试

- 首次 Review 通过后编写或补充测试
- 覆盖核心业务规则、边界条件、异常场景
- 纯工具类、枚举、领域规则优先编写不依赖 Spring 的单元测试
- Application Service 用 `@SpringBootTest` + Mockito mock 集成层
- Mapper / XML SQL / Flyway 迁移用 Testcontainers（PG + Redis），含 MyBatis-Plus Interceptor 链路
- 涉及 Gmail / 飞书 / Kimi 等外部依赖时用 fixture mock，**不依赖真实账号**
- 缺少测试基础设施时优先补最小必要测试，不为测试引入过重框架或大范围重构

## 8. 执行测试与修复

- 测试编写完成后运行与本次改动相关的最快测试
- 影响范围较大时运行更完整的测试或构建检查
- 推荐命令：

  ```bash
  # 单模块测试（含依赖模块）
  mvn -pl maildesk-application -am test

  # 全量构建
  mvn -B verify

  # 含 Testcontainers 的集成测试
  mvn -B verify -Pintegration
  ```

- 测试不通过分析失败原因并继续修改
- 修改后必须重新执行相关测试，直到通过或确认失败由外部环境导致
- 测试因外部环境缺失无法运行时，在交付说明写明原因、替代检查和剩余风险

## 9. 项目知识库更新

- 测试通过后、Git 提交前，根据本次改动更新 `kol-mail-desk-v2-docs/specs/` 的相关文档
- 知识库内容沉淀长期有效信息：

  | 改动类型 | 更新位置 |
  |----------|----------|
  | 新增或调整的业务规则 | `05-feature-parity.md` 或对应 spec |
  | 新接口 | `api-contract-v1.yaml` |
  | 重要数据流、模块协作 | `01-architecture.md` 或 `02-backend-design.md` |
  | 关键配置、外部依赖、环境要求 | 对应 spec |
  | 关键决策 | 新增 ADR（`specs/decisions/ADR-{n}-{slug}.md`） |
  | 风险沉淀 | `07-risks.md` |
  | 排障经验 | 对应 spec 末尾或新增 troubleshooting 文档 |

- 不写入临时过程、短期 TODO、敏感信息、token、账号密码、连接串
- 本次改动无适合沉淀的长期知识时，在交付说明写明「本次无需更新文档」及原因
- Git 提交时把相关文档一并纳入

## 10. Git 提交

- 单元测试通过后才能提交
- 提交前检查工作区，确认只包含本次需求相关文件
- 不提交 `target/`、日志、临时文件、IDE 私有配置、敏感信息
- 提交信息使用 **Conventional Commits**：

  ```
  feat(workbench): add stage filter to GET /api/v1/workbench
  fix(gmail-sync): preserve is_read on update
  refactor(audit): extract AuditLogger AOP
  docs(spec): clarify scheduled email retry rule
  chore(deps): bump spring-ai to 1.0.0-RC1
  test(application): cover team pool reassignment
  ```

- 仅本地提交，**不推送远端**
- 未经开发者明确要求，不进行破坏性 Git 操作（`git reset --hard`、`git push --force` 等）

## 11. 交付说明

- 开发结束后向开发者提供简洁交付说明，包含：
  - 本次实现摘要
  - 修改文件列表
  - 需求分析和技术方案确认情况
  - 首次代码 Review 结论
  - 测试命令和结果
  - **OpenAPI 契约更新位置**
  - **项目文档更新位置**
  - Git 本地提交结果
  - 未解决问题、环境限制、剩余风险

## 流程状态流转

```text
需求输入
  → 需求分析（核对 v3.3 + 重构 spec）
  → 开发者确认
  → 技术方案（含 OpenAPI 改动）
  → 开发者确认
  → 开发任务计划
  → 开发实现（遵守 project-structure + coding-standards）
  → 代码 Review（含 ArchUnit、多租户、审计、集成边界）
  → 编写单元测试
  → 执行测试与修复
  → 文档更新（specs/）
  → Git 提交（Conventional Commits）
  → 交付说明
```

任一阶段未通过应回到对应阶段修正，不允许跳过失败项直接进入后续阶段。

## 例外情形

| 情形 | 简化流程 |
|------|----------|
| 紧急生产 bug 修复 | 跳过任务计划，但 Review + 测试 + 文档不可省 |
| 文档 / typo 修复 | 跳过需求分析和技术方案 |
| 依赖升级 | 必须有需求分析（影响面）和测试 |
| 实验性 spike | 在独立分支，不入主线，不要求完整流程 |
