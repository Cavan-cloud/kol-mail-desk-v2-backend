# cursor-rules-staging

The agent that bootstrapped this repo could not write a `.cursor/` directory
(the sandbox blocks creation of any `.cursor` path). The two `.mdc` rule files
are staged here so you can install them with one command:

```bash
mkdir -p .cursor && mv cursor-rules-staging .cursor/rules
```

After running that, the layout matches the spec:

```
.cursor/
└── rules/
    ├── 00-global.mdc
    └── backend-java.mdc
```

## Rule scope

`backend-java.mdc` 只保留核心硬约束摘要，详细规则在仓库内：

- `docs/standards/project-structure.md` — Maven 多模块工程结构
- `docs/standards/coding-standards.md` — Java 21 + Spring Boot 编码规范
- `docs/standards/dev-workflow.md` — 开发流程 10 步

跨仓库规格在 `../../kol-mail-desk-v2-docs/specs/`（架构、Phase、Feature Parity、OpenAPI）。

## 演化策略

- 项目演进过程中新规则先沉淀到 `docs/standards/`
- 高频违反或安全相关条目晋升到 `.cursor/rules/backend-java.mdc`
- 全局 / 跨语言规则进 `00-global.mdc`
