# 编码规范

约束 maildesk 后端代码编写方式。所有新增/修改优先遵循本文档；与现有代码冲突时优先保持小范围一致，并在必要时逐步收敛。

## 基础原则

- 编写清晰、直接、可测试的代码，不为未来不确定的需求提前设计复杂抽象
- 改动范围最小化，不做与当前需求无关的重构、格式化或依赖升级
- 不提交 `target/`、本地临时文件、IDE 私有配置、日志文件和敏感信息
- 不硬编码账号、密码、token、数据库连接、API Key、Secret 等敏感信息（用环境变量或 Secrets Manager）
- 新增逻辑应能说明输入、输出、异常和边界条件
- **多租户**：所有业务表写入必须带 `tenant_id`

## Java 版本与语言特性

- 项目使用 **Java 21**
- 可以使用 records、sealed classes、pattern matching、virtual threads（验证性能后）
- 集合为空返回空集合（`Collections.emptyList()` / `List.of()`），不返回 `null`
- 字符串判空：`StringUtils.isBlank()`（Apache Commons）或 `String.isBlank()`
- 金额、报价、比例等精确数值使用 `BigDecimal`，不使用 `double` / `float`
- 时间使用 `Instant` / `OffsetDateTime`（对齐 PG `timestamptz`），不使用 `Date` / `Calendar`
- 邮箱归一化：`email.trim().toLowerCase()`（与 PG `normalized_email` 生成列一致）
- 不为语法新颖牺牲可读性

## 命名规范

- 类名大驼峰：`WorkbenchService`、`GmailSyncServiceImpl`、`FeishuSheetClient`
- 方法名、变量名小驼峰：`queryWorkbench()`、`feishuOperatorName`
- 常量大写下划线：`DEFAULT_PAGE_SIZE`、`MAX_HISTORY_PAGES`
- 包名全部小写，根包 `com.lovart.maildesk`
- 后缀表达角色：`Controller`、`Service` / `ServiceImpl`、`Mapper`、`Request` / `Response`、`Dto`、`Enum`、`Config`、`Job`、`Client`
- 布尔变量与方法用 `is` / `has` / `can` / `should` 前缀
- 避免无意义缩写；业务通用缩写如 `DTO`、`KOL`、`AI`、`OAuth` 可保留

## 分层编码规范

| 层 | 职责 | 模块 |
|----|------|------|
| Controller | 接收参数、触发校验、调用 Application Service、包装返回 | `maildesk-api` |
| Application Service | 用例编排、事务边界、跨领域协作 | `maildesk-application` |
| Domain Service | 领域规则、无外部依赖 | `maildesk-domain` |
| Mapper | 数据库访问，不含业务决策 | 接口在 `maildesk-domain` / XML 在 `maildesk-infrastructure` |
| Integration Client | 外部系统访问（Gmail / 飞书） | `maildesk-integration` |
| AI Service | LLM 调用 + Prompt | `maildesk-ai` |

- Controller 不直接调用 Mapper、Integration、AI
- Mapper 不依赖 Service 或 Controller
- 实体（Entity / DO）对应表结构；Request / Response 面向接口；Dto 用于服务层与跨模块传递，不混用职责
- 工具类保持无状态、低耦合，不依赖业务 Service

## Controller 规范

- 使用 `@RestController` + 模块级 `@RequestMapping("/api/v1/...")`
- 新增接口必须补充 `@Operation`，模块类补充 `@Tag`
- 请求体使用 `@Valid @RequestBody`；查询参数使用 `@RequestParam`
- 普通接口统一返回 `ApiResponse.success(data)`
- 分页接口返回 `ApiResponse<PageResponse<T>>`
- 文件下载、流式接口直接写 `HttpServletResponse`，必须设置正确的 `Content-Type`、文件名编码、响应头
- 不在 Controller 中吞异常，由 `GlobalExceptionHandler` 统一处理

## API 契约同步

- **每个新增或修改的接口**必须更新 `kol-mail-desk-v2-docs/specs/api-contract-v1.yaml`
- OpenAPI 是前后端**唯一真实契约**
- 前端 TypeScript 类型从 OpenAPI 自动生成
- 仅改实现不改契约会导致前端类型不一致，PR Review 拒绝合并

## 参数校验规范

- Request 对象使用 Jakarta Validation 注解（`@NotNull`、`@Size`、`@Email`、`@Pattern`）
- Service 层对关键业务条件继续显式校验，不能只依赖 Controller 入参校验
- 业务校验失败用 `Assert` 工具或抛 `BusinessException`
- 对外部传入的 ID、日期、枚举值、分页参数必须校验合法性
- 分页请求统一 `pageIndex`（从 1 开始）+ `pageSize`（默认 20，上限 100）

## 异常处理规范

- 可预期业务错误使用 `BusinessException`（包含 `errorCode` + 中文 `message`）
- 鉴权 / 授权错误用 Spring Security 自带异常（`AuthenticationException` / `AccessDeniedException`）
- 不向前端暴露底层异常、SQL、连接串、外部系统响应中的敏感内容
- 捕获后必须有明确处理：转换、补偿、降级或记录后继续；**禁止空 `catch`**
- 全局异常统一在 `GlobalExceptionHandler` 扩展，不在 Controller 重复写

## 日志规范

- 类级日志使用 Lombok `@Slf4j`
- 日志包含定位问题所需的业务标识：`tenantId`、`userId`、`kolId`、`emailId`、`syncJobId`
- **禁止打印**：完整 token、refresh_token、密码、API Key、邮件正文、AI Prompt 全文、手机号、银行卡号、数据库连接串
- 异常日志保留异常对象：`log.error("Gmail 同步失败，userId={}", userId, ex);`
- 大批量数据日志采样或统计数量，避免输出完整列表
- 禁止使用 `System.out.println` 输出业务日志

## 事务规范

- 多次数据库写入需要原子性时必须加事务
- `@Transactional(rollbackFor = Exception.class)` 放在 **Application Service 实现类**的 public 方法上
- 只读查询不开启写事务
- **事务中禁止**：长时间文件处理、远程 HTTP / Gmail / 飞书 / AI 调用、大批量导出
- 同步任务（Gmail / 飞书）按页处理，每页独立事务

## 数据访问规范（MyBatis-Plus + Flyway）

ORM 决策见 [ADR-006](../../../kol-mail-desk-v2-docs/specs/decisions/ADR-006-orm-mybatis-plus.md)。**全项目统一 MyBatis-Plus 3.5+，禁止混入 JPA / Hibernate / hibernate-types**。

### 通用

- 单数据源 PostgreSQL 16
- ORM 框架：MyBatis-Plus 3.5+；XML Mapper + 注解式条件构造组合使用
- 实体类（DO）放 `maildesk-domain`，使用 MyBatis-Plus 注解（`@TableName` / `@TableId` / `@TableField` / `@TableLogic` / `@Version`），**不使用 `@Entity` / `@Column`**
- Mapper 接口放 `maildesk-domain`，继承 `BaseMapper<T>`；XML 放 `maildesk-infrastructure/src/main/resources/mapper/`
- 复杂查询写在 XML 内的原生 SQL（CTE、窗口函数、`RETURNING`、`ON CONFLICT` 自由使用）
- **业务代码不直接使用 `JdbcTemplate`**；MyBatis 不便表达的极端场景才退到 `JdbcTemplate`，必须代码注释说明原因

### 实体（DO）规范

- 类名以 `DO` 结尾或保持与表语义一致（如 `KolDO` / `EmailDO` / `ScheduledEmailDO`）
- 主键统一 `id`（UUID 或 BIGINT，根据表设计），用 `@TableId(type = IdType.ASSIGN_UUID)` 或对应策略
- 软删除字段：`deleted_at TIMESTAMPTZ`，标注 `@TableLogic(value = "null", delval = "now()")`
- 乐观锁字段：`version INTEGER`，标注 `@Version`
- 审计字段：`created_at` / `updated_at` / `created_by` / `updated_by`，由 `MetaObjectHandler` 自动填充，写代码时**不显式 set**
- 多租户字段：`tenant_id`，由 `TenantLineInnerInterceptor` 自动注入，**Mapper 中不写 `WHERE tenant_id = ?`**
- 数据库列名用 `snake_case`，Java 字段用 `camelCase`，靠 `MyBatisPlusConfig` 的 `mapUnderscoreToCamelCase=true` 自动映射

### Mapper 规范

- 接口位于 `com.lovart.maildesk.domain.{module}.mapper.XxxMapper`
- 简单 CRUD 用 `BaseMapper` 内置方法 + `LambdaQueryWrapper`，**禁止裸 `QueryWrapper`**（无类型安全）
- 多表 JOIN、聚合、子查询、`UNION`、CTE 全部写在 XML
- XML 文件名与 Mapper 接口名一致（如 `EmailMapper.xml`），`namespace` 必须与接口全限定名一致
- XML SQL 使用 `<![CDATA[...]]>` 包裹特殊字符，注意比较运算符 `<` `>` 的转义
- 单条 SQL 不要超过 80 行；超出拆分子查询或视图（Flyway 创建）

### SQL 安全

- 参数绑定使用 `#{name}`，**禁止 `${name}`**（除白名单字段名 / 排序列外）
- 动态字段或排序必须做白名单校验（在 Service 层判断后传入）
- 显式列出查询字段，避免在复杂查询用 `SELECT *`
- 多表查询用清晰别名，避免字段重名导致映射错误
- 表名 / 列名引用使用 `"` 包裹仅当涉及大小写敏感时（PG 默认转小写）

### 分页

- 简单分页：`IPage<T>` + `Page<T>` + `PaginationInnerInterceptor`
- 复杂分页：XML 中的 `<select>` 同样支持 `IPage` 自动分页（MyBatis-Plus 拦截器织入 `LIMIT ... OFFSET ...` 与 `SELECT COUNT(*)`）
- 看板大数据：用 `COUNT(*) OVER ()` 单次返回，结果映射到自定义 `BoardPageVO`
- 统一分页参数：`pageIndex`（从 1 开始）+ `pageSize`（默认 20，上限 100）

### 批量操作

- 批量 upsert 用 PostgreSQL `ON CONFLICT ... DO UPDATE`，在 XML 内 `<insert>` + `<foreach>` 实现
- 批量 insert 优先 `IService#saveBatch`（默认每批 1000），可在 `MyBatisPlusConfig` 调整
- 控制批次大小，避免一次性加载或写入过多数据
- Gmail 同步批量插入 ≤ 100 条/批；超过拆批，每批独立事务

### PG 特有类型（JSONB / Array / ENUM）

- `JSONB` 字段（`ai_extracted_fields` / `actions.metadata` / Gmail headers）使用 `JsonbTypeHandler`（`maildesk-common/typehandler/`），字段类型为强类型 DTO 或 `Map<String, Object>`
- `TEXT[]` 字段（`to_emails` / `cc_emails` / `feishu_tags`）使用 `StringArrayTypeHandler`，字段类型为 `List<String>`
- PG `ENUM` 类型（`kol_stage` 等）使用 `PgEnumTypeHandler<E>`，字段类型为 Java 枚举
- 在 Mapper XML 内对 JSONB / Array 字段需显式声明 `typeHandler`，例如：
  ```xml
  <result column="ai_extracted_fields" property="aiExtractedFields"
          typeHandler="com.lovart.maildesk.common.typehandler.JsonbTypeHandler"/>
  ```

### Interceptor 链路（在 `MyBatisPlusConfig` 注册，顺序敏感）

1. `TenantLineInnerInterceptor` — 多租户自动注入
2. `OptimisticLockerInnerInterceptor` — 乐观锁
3. `PaginationInnerInterceptor` — 分页
4. `BlockAttackInnerInterceptor` — 拦截全表 UPDATE / DELETE（无 WHERE 直接抛错）

### 迁移

- 所有 schema 变更必须通过 Flyway 迁移
- 文件命名 `V{n}__{description}.sql`，`n` 单调递增
- DDL 由 Flyway 全权负责；**MyBatis-Plus 启动时不生成任何 DDL**
- 启动期对照 schema 校验交由 Flyway 自身 + CI 集成测试覆盖（无 JPA `validate` 模式可用）

## API 响应规范

- 成功响应：`ApiResponse.success(data)`
- 失败响应：异常机制统一处理，业务代码不手写失败响应
- 分页：`PageResponse`（`content`、`pageIndex`、`pageSize`、`totalCount`）
- Response 只暴露前端需要字段，**不直接返回 DO**
- 敏感字段按权限脱敏或隐藏；**OAuth Token 永不返回前端**

## 配置规范

- 环境差异：`application-{dev,test,prod}.yml`
- 公共默认：`application.yml`
- 新增配置必须有清晰前缀：`maildesk.gmail.*`、`maildesk.feishu.*`、`maildesk.ai.*`
- 复杂配置用 `@ConfigurationProperties`，不堆 `@Value`
- **敏感配置**（Google Client Secret、Kimi Key、AES Key、DB 密码）走环境变量或 Secrets Manager，**不写在 yml**
- 本地路径、外部地址、超时时间、开关必须可配置

## 多租户规范

- 所有业务表必须有 `tenant_id` 列
- 查询路径：由 MyBatis-Plus `TenantLineInnerInterceptor` 自动追加 `WHERE tenant_id = ?`，**Mapper 中禁止手写 `tenant_id` 过滤**（重复且容易冲突）
- 写入路径：由 Interceptor 自动注入 `tenant_id`，Service 层**禁止显式 set tenant_id**
- 从 `TenantContext`（ThreadLocal）获取当前租户，由 `TenantLineHandler` 实现读取
- 跨租户操作仅限平台管理后台（Phase 7），需绕开拦截器，必须显式注解 `@IgnoreTenant` 并写 audit log
- Phase 7 启用 PostgreSQL RLS 兜底（应用层 + DB 双重防护）
- Worker 任务执行前从消息 payload 重建 `TenantContext`，任务结束 `finally` 清理

## 集成层规范（Gmail / 飞书 / AI）

- 集成必须实现 `maildesk-domain` 定义的接口
- 不允许业务层直接 `import com.google.api.*` 或 `com.lark.oapi.*`
- **飞书严格只读**，禁止调用任何写 API
- **AI 永不自动发信**，所有 AI 输出仅作为草稿
- OAuth Token：AES-256 加密存储 `integration_credentials.encrypted_payload`
- 外部调用必须有超时（默认 10s）和重试（最多 3 次 + 指数退避）
- 外部响应必须校验空值、错误码、字段格式
- 对外部失败的处理策略明确：失败中断、降级返回、重试或记录后继续，**禁止静默失败**

## 审计规范

- **所有写操作必须写 audit log**（`actions` 表，append-only）
- 字段：`tenant_id`、`actor_user_id`、`action_type`、`target_type`、`target_id`、`metadata`、`created_at`
- 使用 `AuditLogger` Bean，可通过 AOP 自动织入

## 文件与模板处理规范

- Word、Excel、PDF、图片等二进制资源放在对应模块 `src/main/resources/templates/` 或明确资源子目录
- **不要对二进制模板启用 Maven 文本过滤**
- 文件下载名必须进行安全处理，避免非法字符和响应头注入
- 文件流使用 try-with-resources 管理，避免资源泄露
- 大文件处理考虑大小限制、临时目录、超时、并发控制

## 并发与上下文规范

- 当前用户通过 `UserContext` / `SecurityContextHolder` 获取
- `ThreadLocal` 后必须确保清理（filter / interceptor 的 `finally` 或 `afterCompletion`）
- 共享可变状态保证线程安全；Spring 单例 Bean 中不保存请求级可变数据
- 定时任务、Worker、异步任务中不能假设存在 HTTP 请求上下文
- Worker 任务执行前从消息 payload 重建 `TenantContext`

## 测试规范

- 行为变更应新增或更新测试
- 纯工具类、枚举、领域规则优先编写不依赖 Spring 的单元测试
- Application Service 用 `@SpringBootTest` + Mockito mock 集成层
- Mapper、Flyway 迁移用 Testcontainers（PG + Redis），含 MyBatis-Plus Interceptor 链路验证
- 测试命名描述场景和期望结果：`should_filter_orphan_kol_for_leader_when_view_pool()`
- 测试数据最小化，不依赖线上真实数据
- 无法运行测试时，在交付说明中写明原因

## 代码提交前检查

- 没有提交敏感信息、本地临时文件、`target/` 构建产物
- 新增接口有参数校验、Swagger 注解、统一响应
- 新增接口已更新 `kol-mail-desk-v2-docs/specs/api-contract-v1.yaml`
- 新增 Mapper 与 DO / XML SQL 一致，且经 Testcontainers 集成测试覆盖
- 异常、日志、事务、审计处理符合本文档
- 多租户字段（`tenant_id`）、敏感字段处理符合规范
- 运行与改动相关的最快测试：`mvn -pl <module> -am test`
