# 工程结构

## 概览

maildesk 后端为 **Maven 多模块工程**，按职责拆为 8 个模块。模块依赖单向流动，由 ArchUnit 守护。

## 仓库根目录

| 路径 | 用途 |
|------|------|
| `pom.xml` | 父 POM，统一依赖管理与构建插件，定义 `dev` / `test` / `prod` 三套 profile |
| `README.md` | 项目说明 |
| `AGENTS.md` | Agent 协作契约 |
| `.gitignore` | Git 忽略规则 |
| `docs/standards/` | 项目规则（本目录） |
| `.cursor/rules/` | Cursor Agent 规则 |
| `maildesk-*/` | 各 Maven 模块 |
| `target/` | Maven 构建产物，不允许手工编辑或提交其中内容 |

## Maven 模块（8 个）

```
maildesk/
├── maildesk-common          # DTO、枚举、异常、工具
├── maildesk-domain          # 实体（DO）、Mapper 接口、领域服务接口、领域规则
├── maildesk-infrastructure  # MyBatis-Plus 配置、Mapper XML、Redis、加密、OAuth Token 存储
├── maildesk-integration/
│   ├── gmail               # Gmail API
│   └── feishu              # 飞书 Sheet 同步
├── maildesk-ai              # Spring AI：classify/draft/check/translate
├── maildesk-application     # 用例服务（ApplicationService）
├── maildesk-api             # REST Controller、Spring Security（HTTP 启动类）
└── maildesk-worker          # 定时任务、队列消费（独立启动类）
```

## 模块职责详细

### `maildesk-common`

- DTO、枚举、异常基类、工具方法
- 不依赖任何业务模块
- 子包：`common.dto`、`common.enums`、`common.exception`、`common.util`

### `maildesk-domain`

- 业务实体（DO，使用 MyBatis-Plus 注解 `@TableName` / `@TableId` / `@TableField` / `@TableLogic` / `@Version`）
- Mapper 接口（继承 `BaseMapper<T>`，不含 XML）
- 领域服务接口
- 领域规则（pure logic，可独立测试）
- 不依赖 infrastructure、integration、application
- 子包：`domain.kol`、`domain.email`、`domain.template`、`domain.team`、`domain.audit`
- 每个子包下进一步分：`entity/`（DO）、`mapper/`（接口）、`service/`（领域服务接口）

### `maildesk-infrastructure`

- MyBatis-Plus 配置（`MyBatisPlusConfig`：Interceptor 链路、`MetaObjectHandler`、`TenantLineHandler`）
- Mapper XML 文件（`resources/mapper/*.xml`）
- 自定义 TypeHandler 注册（`JsonbTypeHandler`、`StringArrayTypeHandler`、`PgEnumTypeHandler`）
- Redis 客户端封装
- OAuth Token 加密存取（AES-256）
- S3 对象存储客户端
- 子包：`infrastructure.config`、`infrastructure.redis`、`infrastructure.crypto`、`infrastructure.storage`

### `maildesk-integration/{gmail, feishu}`

- 外部系统访问
- 必须实现 `maildesk-domain` 定义的接口
- 不允许业务层直接 `import com.google.api.*` 或 `com.lark.oapi.*`
- 子包：`integration.gmail`、`integration.feishu`

### `maildesk-ai`

- Spring AI `ChatClient`（OpenAI 兼容 → Kimi/Moonshot）
- Prompt 模板（`resources/prompts/*.st`）
- AI 降级 fallback（无 Key 或调用失败）
- 子包：`ai.classify`、`ai.draft`、`ai.check`、`ai.translate`、`ai.fallback`

### `maildesk-application`

- 用例编排（多领域协作）
- 事务边界
- 调用 domain、integration、ai
- 子包：`application.workbench`、`application.board`、`application.sync`、`application.send`、`application.team`、`application.template`、`application.scheduled`

### `maildesk-api`

- REST Controller（`@RestController`）
- Spring Security + OAuth2
- OpenAPI / Swagger
- HTTP 启动类（`MaildeskApiApplication`）
- 不写业务逻辑，只编排 Application Service
- 子包：`api.controller`、`api.security`、`api.config`、`api.interceptor`、`api.filter`

### `maildesk-worker`

- 独立 Spring Boot 启动类（`MaildeskWorkerApplication`），不暴露业务 HTTP（仅 actuator）
- 定时任务（Spring Scheduler + Redis 分布式锁）
- 队列消费（Redis Streams / Kafka）
- 共享 application、domain、infrastructure
- 子包：`worker.gmail`、`worker.feishu`、`worker.scheduled`、`worker.lock`

## 模块依赖方向

```
api / worker
    ↓
application
    ↓
domain ← integration / ai
    ↓
infrastructure
    ↓
common
```

依赖规则：

- **只能从上到下依赖**
- domain 是核心，不依赖外部
- integration、ai、infrastructure 都实现 domain 的接口
- **由 ArchUnit 测试守护**，违反直接编译失败（`mvn verify` fail）

## 包名约定

根包：`com.lovart.maildesk`

按模块划分子包，**不要按角色（controller / service / dao）跨模块**。

| 模块 | 子包前缀 |
|------|---------|
| `maildesk-common` | `com.lovart.maildesk.common.*` |
| `maildesk-domain` | `com.lovart.maildesk.domain.*` |
| `maildesk-infrastructure` | `com.lovart.maildesk.infrastructure.*` |
| `maildesk-integration/gmail` | `com.lovart.maildesk.integration.gmail.*` |
| `maildesk-integration/feishu` | `com.lovart.maildesk.integration.feishu.*` |
| `maildesk-ai` | `com.lovart.maildesk.ai.*` |
| `maildesk-application` | `com.lovart.maildesk.application.*` |
| `maildesk-api` | `com.lovart.maildesk.api.*` |
| `maildesk-worker` | `com.lovart.maildesk.worker.*` |

## 数据访问

- 主库：**PostgreSQL 16**（单数据源）
- ORM：**MyBatis-Plus 3.5+**（简单 CRUD 用 `BaseMapper` + `LambdaQueryWrapper`；复杂 SQL 写在 XML）
- **不使用 JPA / Hibernate / hibernate-types**（详见 [ADR-006](../../../kol-mail-desk-v2-docs/specs/decisions/ADR-006-orm-mybatis-plus.md)）
- **业务代码不直接使用 `JdbcTemplate`**，复杂查询统一在 Mapper XML 里写原生 SQL
- 迁移：**Flyway**，文件命名 `V{n}__{description}.sql`
- 索引：与查询模式对齐（如 `(user_id, sent_at DESC)`、`(normalized_email, feishu_operator_name)`）

## 资源目录

| 路径 | 用途 |
|------|------|
| `maildesk-api/src/main/resources/application.yml` | API 公共配置 |
| `maildesk-api/src/main/resources/application-{dev,test,prod}.yml` | API 环境配置 |
| `maildesk-worker/src/main/resources/application.yml` | Worker 配置 |
| `maildesk-infrastructure/src/main/resources/db/migration/V*.sql` | Flyway 迁移 |
| `maildesk-infrastructure/src/main/resources/mapper/*.xml` | MyBatis-Plus Mapper XML（按模块子目录：`mapper/kol/`、`mapper/email/` …） |
| `maildesk-ai/src/main/resources/prompts/*.st` | Spring AI Prompt 模板 |
| `maildesk-api/src/main/resources/templates/` | 邮件模板、导出模板（按业务子目录） |

## 测试目录

- 测试代码：每个模块的 `src/test/java`，包名与被测代码对应
- 测试资源：`src/test/resources`
- 单元测试：JUnit 5 + AssertJ + Mockito
- 集成测试：Testcontainers（PostgreSQL + Redis）
- ArchUnit 测试：放在 `maildesk-domain/src/test/java/.../arch/`
- E2E：Postman / Newman 集合，由 CI 跑

## 新增功能放置规则

| 新增内容 | 放置位置 |
|----------|----------|
| REST 接口 | `maildesk-api/.../controller/`；请求 → `common.dto.request`；响应 → `common.dto.response` |
| 业务流程 | 接口在 `maildesk-domain/.../service/`，实现在 `maildesk-application/.../service/` |
| 数据库实体（DO） | `maildesk-domain/.../{module}/entity/XxxDO.java` |
| 数据库 Mapper 接口 | `maildesk-domain/.../{module}/mapper/XxxMapper.java`（继承 `BaseMapper<XxxDO>`） |
| 复杂 SQL（多表 / 聚合 / upsert） | `maildesk-infrastructure/.../resources/mapper/{module}/XxxMapper.xml` |
| 数据库表 / 字段 | `maildesk-infrastructure/.../db/migration/V{n}__{desc}.sql` |
| 自定义 TypeHandler | `maildesk-common/.../typehandler/`，并在 `MyBatisPlusConfig` 注册 |
| MyBatis-Plus 拦截器 | `maildesk-infrastructure/.../config/MyBatisPlusConfig.java` |
| 业务状态 | 枚举放 `common.enums`，避免散落魔法字符串 |
| 外部系统封装 | `maildesk-integration/gmail` 或 `maildesk-integration/feishu` |
| AI 能力 | `maildesk-ai`，Prompt 进 `resources/prompts/` |
| 定时任务 | `maildesk-worker` |
| 通用工具 | `common.util`，确认已有工具无法复用后再加 |

## 关键禁止

- ❌ Controller 直接调 Mapper 或 Integration
- ❌ Mapper 依赖 Service 或 Controller
- ❌ `maildesk-domain` 依赖 `infrastructure` / `integration` / `ai` / `application`
- ❌ 业务代码 `import com.google.api.*` 或 `com.lark.oapi.*`（必须通过 domain 接口）
- ❌ 把业务逻辑写在 Controller
- ❌ 在 `target/` 编辑或提交内容
- ❌ 引入 JPA / Hibernate / hibernate-types / `spring-boot-starter-data-jpa`（统一 MyBatis-Plus）
- ❌ 业务代码直接 `import org.springframework.jdbc.core.JdbcTemplate`（特殊场景例外，需注释说明）
- ❌ Mapper 中手写 `WHERE tenant_id = ?`（由 `TenantLineInnerInterceptor` 自动注入）
- ❌ 在 Service 显式 set `created_at` / `updated_at` / `tenant_id`（由 Interceptor / `MetaObjectHandler` 自动填充）
- ❌ 引入 Apollo（用 `application.yml` + Secrets Manager）
- ❌ 业务层调用 Gmail/飞书写 API（飞书严格只读）
- ❌ Spring AI 输出自动触发发信（永远人工确认）
