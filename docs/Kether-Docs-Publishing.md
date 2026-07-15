# Kether 文档机器发布与 CI/CD

Orryx 从运行时完整注册表生成 Kether 文档，并通过 GitHub Pages 发布为可验证的机器数据供应链。Markdown 面向用户，JSON 是 Orryx Editor 等消费者的唯一机器事实源。

## 发布通道

- `kether/channels/stable.json`：只由 `vA.B.C` Tag 更新，供生产编辑器读取。
- `kether/channels/snapshot.json`：由 `master` 更新，用于开发验证。
- Pull Request 只生成和校验候选包，不部署。

通道指针只包含版本、完整 Git SHA、`releaseId` 和不可变发布清单路径。消费者应优先使用条件请求检查该小文件。

## 不可变目录

正式版：

```text
kether/releases/<version>/<full-commit-sha>/
```

开发快照：

```text
kether/snapshots/<full-commit-sha>/
```

每个目录包含：

```text
manifest.json
 kether-registry.json
 kether-registry.schema.json
 actions-schema.json
 actions-schema.schema.json
 docs.md
 changes.json
 checksums.json
```

已经存在的不可变目录不能被不同内容覆盖。相同 Tag 重跑时，内容一致则幂等通过，内容不同则 CI 失败。

## Registry Schema v4 与 v3 兼容层

`kether-registry.json` 使用 `schemaVersion: 4`，是新消费者的完整机器事实源；`actions-schema.json` 继续保留 `version: 2`、`schemaVersion: 3`，兼容旧客户端。

v4 Registry 提供：

- 完整类型枚举、父/子集合、`assignableFrom`、JVM `rawType` 与 Kether expression 可填充性。
- Action 的结构化 aliases、namespace/shared、keyword alternatives、有限值输入 `options`、output 状态、execution contexts、requirements、source、grammar/variants。
- Trigger 的 eventClass/cancellable，以及字段 aliases、readable/writable/nullable/rawType/ketherFillable。
- Action、Selector、Trigger、Property 的稳定 ID、插件版本和完整构建 commit。

业务 JSON 不包含生成时间，确保同一源码可以确定性重建。时间只写入 Manifest。Release Manifest 的 `schemaVersion` 为 `4`，`minimumEditorSchemaVersion` 为 `3`，表示发布包仍携带 v3 兼容资产。

## Kether 源码注释约定

Orryx 的技能、中转站、状态、占位符、临时脚本及表达式入口共享统一预处理器。预处理顺序固定为：

1. 扫描源码并移除单双引号外、未转义 `#` 开始的内容。
2. 保留注释行和行尾注释原有的 LF/CRLF 换行，避免后续脚本行号整体偏移。
3. 在注释处理完成后，检查首个非空内容是否为完整 `def` 词元；不是时才包装 `def main`。

单双引号内的 `#`、转义引号及转义 `#` 不会被当作注释。纯注释脚本会生成一个内容为空但可正常加载的 `main` 函数。

## 本地生成

默认生成 snapshot：

```bash
./gradlew generateKetherDocs
node scripts/validate-kether-docs.mjs build/generated-docs
```

生成 stable 候选：

```bash
KETHER_DOCS_CHANNEL=stable ./gradlew generateKetherDocs
node scripts/validate-kether-docs.mjs build/generated-docs
```

可选环境变量：

```text
KETHER_DOCS_CHANNEL=stable|snapshot
KETHER_DOCS_GENERATED_AT=<RFC3339 UTC>
KETHER_DOCS_PREVIOUS_SCHEMA=<上一版 actions-schema.json>
KETHER_DOCS_PREVIOUS_RELEASE_ID=<上一版 releaseId>
```

`validateKetherDocs` Gradle 任务会在生成后运行同一验证器：

```bash
./gradlew validateKetherDocs
```

## CI/CD 顺序

1. 解析版本、channel、commit 和 releaseId。
2. 获取历史分支中的上一 stable Schema，用于生成结构化差异。
3. 启动临时 Paper，等待所有 Kether 注册完成并生成候选包。
4. 校验 JSON、稳定 ID、类型/分类引用、数量、大小预算、bytes 和 SHA-256。
5. 将候选包安全合并到 `kether-docs` 历史分支。
6. 不可变目录存在且内容不同则拒绝发布。
7. 从完整历史分支生成 Pages artifact 并原子部署。
8. 部署后从公网通道指针开始重新下载所有资产并校验 SHA-256。

Pages artifact 会替换整站，因此 `kether-docs` 分支负责保存历史发布目录。线上切换发生在 Pages deployment；部署失败不会破坏之前的线上 stable。

## 回滚

回滚只移动 `channels/stable.json` 到一个已存在且校验通过的旧 releaseId。不要修改、重建或删除旧 release 目录。

## 兼容 URL

迁移期间继续发布：

```text
kether/manifest.json
kether/kether-registry.json
kether/actions-schema.json
kether/latest.md
kether/versions/<version>.md
```

新消费者应读取 `channels/stable.json`。旧 URL 仅作为兼容层，不具备不可变发布语义。

## 安全约束

- GitHub Actions 固定到完整 40 位 commit SHA。
- PR 工作流只读，不接触发布权限。
- 发布资产路径只能是当前 release 目录内的安全相对文件名。
- SHA-256 对实际发布的 UTF-8 文件字节计算。
- 文档 Markdown 是不可信文本，消费者不得执行其中的 HTML 或脚本。
- source 信息只能使用仓库相对路径，不得输出构建机绝对路径。
