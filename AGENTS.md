# Repository Guidelines

## 项目结构与模块组织
- `src/main/kotlin/org/gitee/orryx`：核心插件代码，按 `api/`、`core/`、`module/`、`compat/` 组织。
- `src/main/resources`：默认配置与 UI YML（`ui/dragoncore`、`ui/germplugin` 等）。
- `src/resource`：UI 图片资源（PNG）。
- `docs/`：集成与设计文档；`libs/`：本地依赖 jar。

## 构建、测试与本地开发
- `./gradlew build`：构建发行包。
- `./gradlew taboolibBuildApi -PDeleteCode`：生成 API 包（开发用）。
- `./gradlew dokkaHtml`：生成 API 文档到 `build/<name>-<version>-doc`。
- 运行插件需在 Minecraft 服务器端加载生成的 jar。

## 编码风格与命名
- Kotlin/Java 混合项目，保持 Kotlin 4 空格缩进、`val` 优先、类名 `PascalCase`、函数/变量 `camelCase`。
- 包名以 `org.gitee.orryx` 开头，模块目录与包路径保持一致。
- 资源命名以功能为中心，YAML 使用小写加下划线或现有命名习惯。

## 测试指南
- 目前未配置专用测试框架与覆盖率要求。
- `src/test/kotlin` 主要为手动/基准测试类（如 `main` 入口），建议在 IDE 中直接运行。
- 若新增单测，放置于 `src/test/kotlin` 并说明运行方式。

## 提交与 PR 规范
- 提交信息采用 `type(scope): 摘要` 或 `type（scope）：摘要`，常见类型：`feat`、`fix`、`refactor`、`chore`、`docs`；版本号更新多用 `chore(version)`。
- PR 需说明改动范围、相关模块/配置、兼容性影响，并附最小复现或验证步骤（例如 `./gradlew build`）。涉及 UI/配置变更请附截图或示例配置。

## 配置与安全
- 运行时配置位于 `plugins/Orryx/`（与 `src/main/resources` 的模板对应）；不要把生产密钥提交到仓库。
- 发布相关凭据通过 `gradle.properties` 或 CI 注入，不要硬编码到代码或文档。
