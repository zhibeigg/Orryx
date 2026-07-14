# Orryx Creation Suite 架构

## 1. 范围与事实来源

本目录描述的是 `agent-skills/orryx-creation-suite/shared/orryx_toolkit` 当前可执行代码，而不是期望中的未来实现。合同版本为 `1.0`，共享 Runtime 版本常量为 `1.0.0`。

事实来源按优先级为：

1. `shared/orryx_toolkit/*.py` 的实际分支、默认值和输出。
2. `assets/contracts/*.json` 中面向调用方的生产合同。
3. 本文档对代码与公开合同之间差异的解释。

凡是 Runtime 无法确认的事件名、Kether 动作、客户端资源、第三方插件语义或部署状态，都必须作为前置条件或 `requirements` 返回，不能写成已经验证。

## 2. 共享 Runtime

十个组件并不各自维护一套计算内核。组件技能的 `scripts/run_pipeline.py` 只是薄启动器，它们定位同一份 `orryx_toolkit`，固定 `component` 后调用 `run_contract`。共享 Runtime 的主要层次如下：

| 层 | 模块 | 当前职责 |
|---|---|---|
| 合同层 | `contracts.py` | 规范化输入、构造 artifact/diagnostic/check/reference/requirement、稳定排序、计算输入摘要和最终状态 |
| 分派层 | `orchestrator.py` | 分派单组件，或按 `request.steps` 执行编排、合并结果、检查重复 artifact 和必需引用 |
| 领域层 | `ability.py` 等九个 runner | 生成候选配置或执行静态检查 |
| 工作区层 | `workspace.py` | 发现 Orryx YAML、解析根目录、限制相对路径不能逃逸 root |
| YAML 层 | `yaml_io.py` | 使用 PyYAML safe API，并以稳定字段顺序输出 YAML |
| 写盘层 | `materialize.py` | 显式校验并逐文件写入；默认拒绝覆盖 |
| 私有服务边界 | `service_runner.py` | 校验公开 service envelope、递归拒绝写盘/重载/路径注入意图，并注入可信工作区与 Action Schema |
| CLI 层 | `cli.py` | 提供 `run`、`validate-workspace`、`materialize` 三个命令 |

九个领域 runner 是 `validator`、`kether`、`ability`、`progression`、`job`、`station`、`combat`、`selector`、`ui`。第十个工作流组件 `orchestrator` 负责组合它们。`materialize` 是独立写入边界，也可以作为本地 orchestrator 的最后一步，但只有顶层 `operation=materialize` 且前序没有 error 时才会执行。新增的私有服务入口不改变这条本地能力；它在调用 `run_contract` 前建立更窄的公开合同，服务请求不能到达任何 materialize 分支。

## 3. 数据流与写入边界

```text
UTF-8 JSON
   │
   ▼
normalize_contract
   │  补默认值、检查 contractVersion/component/operation
   ▼
dispatch / run_orchestrator
   │
   ├─ 领域计算与静态检查
   ├─ artifacts（候选内容）
   ├─ diagnostics / checks
   └─ references / requirements
   │
   ▼
finalize_result
   │  稳定排序、status、provenance.inputDigest
   ▼
结果 JSON（不写 Orryx 配置）
   │
   └─ 仅在显式调用 CLI materialize 后才进入写盘流程
```

`generate`、`validate` 和 `plan` 都不能因为结果中出现 `artifacts` 就写盘。`artifacts` 只是候选文件：`path` 表示相对目标，`content` 是候选内容，`sha256` 用于完整性校验。只有显式 CLI `materialize`，或满足安全门的 orchestrator `materialize` 最后一步，才会打开目标文件。

`policy.materialize=true` 与请求中的 `reloadServer=true` 不会自动触发写盘或重载。`run_contract` 仍拒绝直接分派顶层 `component=materialize`；受控写盘入口是 CLI `materialize`，以及顶层 `operation=materialize`、前序零 error 的本地 orchestrator materialize step。

私有服务调用使用 `service_runner.run_service_request`（短别名 `run_service`）和 `assets/contracts/service-runner-envelope.schema.json`。公开 JSON 只包含 `envelopeVersion` 与不带 workspace 的 `contract`；服务宿主通过关键字参数注入 `workspace_root`、`workspace_mode` 和可选可信 `actions_schema`。入口递归扫描整个公开合同，拒绝：

- 顶层或任意 orchestrator step 中的 `component=materialize` / `operation=materialize`；
- `generate/validate/plan` 之外的 operation；
- 任意深度的 `workspace`、`actionsSchemaPath`、任何形式的 `actionsSchema` 与 `reloadServer`；
- 任意 policy 中的 `materialize`；
- materialize 实现会视为允许覆盖的 `overwrite=true/allow/overwrite`。

通过边界后，入口强制注入可信 workspace、强制 `overwrite=deny`，并以内联对象覆盖相关组件 request 中的 Action Schema，避免读取用户指定路径。响应固定为 `envelopeVersion/status/result/errors`：接受并执行后为 `status=completed`，合同边界或基础设施失败为 `status=rejected`，错误使用 Schema 中枚举的稳定 `SERVICE_*` 代码。

## 4. materialize 的真实行为

写盘输入从以下位置依次读取：

1. `request.artifacts`；
2. `request.result.artifacts`；
3. 顶层 `artifacts`（兼容路径）。

写盘前会检查：

- artifact 必须包含字符串 `path` 和 `content`；
- 若提供 `sha256`，必须与 UTF-8 内容一致；
- 路径必须是工作区内的相对路径，拒绝绝对路径、盘符、空段、`.`、`..` 和逃逸 root 的解析结果；
- 同一批次不能出现重复目标；
- 默认 `policy.overwrite=deny`；
- `policy.createParents` 默认为 `true`。

所有可预检错误会在第一次写入前阻断。实际写入时，每个文件先写同目录的 `.<name>.orryx-toolkit.tmp`，执行 flush 和 `fsync`，再用 `os.replace` 替换目标。因此原子性是“单文件级”，不是“整批事务级”。如果后续文件发生临时文件冲突或 I/O 错误，之前已经替换的文件不会自动回滚。生产调用方必须把 `MATERIALIZE_NOT_TRANSACTIONAL` 视为发布前置条件。

Runtime 从不执行 Orryx reload、服务器命令或进程重启。

## 5. orchestrator 的真实行为

`request.steps` 必须是非空数组。每个 step 至少提供 `component` 和 object 类型的 `request`。当前行为是：

1. 先验证 step 结构、组件和 operation，再按固定依赖顺序重排：`validator → kether → ability → progression → job → station → combat → selector → ui → materialize`；同组件的多个 step 保持输入相对顺序。
2. 拒绝 step 递归调用 `orchestrator`。
3. step 的 workspace 默认继承顶层 workspace；policy 是顶层 policy 与 step policy 的浅合并。
4. 领域 step 的 `generate/validate/plan` 当前主要作为合同语义传入；runner 不会因 `plan` 写盘。
5. `materialize` step 要求顶层 `operation=materialize`；执行前先对累计候选做重复路径和必需引用预检，任何 error 都会阻断。step 未显式提供 artifacts 时会自动使用前序累计候选。
6. 合并后再次检查重复 artifact 路径。
7. 合并后检查所有 `required=true` 的引用目标是否存在于候选 artifacts 或工作区文件中。
8. 最终数组会被合同层按稳定键排序，因此“执行顺序”不等于“输出数组顺序”。

单组件分派可用 `policy.validateReferences=false` 跳过引用检查；orchestrator 当前总会执行合并后的必需引用检查，不读取该开关来跳过。

固定依赖顺序同时记录在 `assets/contracts/orchestrator-manifest.json`：

```text
validator → kether → ability → progression → job → station → combat → selector → ui → materialize
```

其中很多依赖是条件性的。例如 Job 只有在引用本次生成的 Experience/Skill 时才依赖 progression/ability，UI 的技能引用当前是可选引用。

## 6. 安全边界

### 6.1 已实现

- YAML 读取使用 `yaml.safe_load`。
- artifact 路径在 materialize 时限制在 `workspace.root` 内。
- 默认拒绝覆盖。
- 提供 artifact SHA-256，并在写盘时校验已提供的摘要。
- validator 会扫描一组敏感键和带凭据 URL/私钥特征。
- 不自动联网，不自动重载服务器。
- 私有服务公开合同不接受 workspace root、文件型 Action Schema、materialize/覆盖/重载意图；可信路径与 Schema 只能由宿主进程注入。
- 私有服务边界错误固定为稳定 `SERVICE_*` code，拒绝响应不会返回未结构化异常或主机路径。
- Ability/Kether 会对非 `1.12.2` 的 Aim 请求给出 error。
- Station/Kether 会提示异步上下文中的 Bukkit 主线程风险；validator 也会对磁盘上的 `Async: true` Station 扫描常见敏感动作。
- 打包的 `actions-schema.json` 由 `scripts/build_action_schema.py` 直接扫描 Kotlin `@KetherParser` 注册点生成，并通过源码摘要做陈旧检测。

### 6.2 仅声明、未统一执行

- `policy.strict` 被规范化并透传，但没有统一的“warning 升级为 error”执行器。
- `policy.network` 默认 `deny`，但 Runtime 没有网络策略沙箱；当前代码本身也不发起网络请求。宿主仍需控制网络。
- `workspace.mode` 被保留，字符串 workspace 简写会设为 `project`，object 默认 `standalone`；当前引用检查不会因 mode 改变严重度。
- `policy.plugins` 被保留，但当前不会自动探测服务器已安装插件。

### 6.3 未实现或只能部分检查

- Kether 检查器是轻量静态扫描器，不是 Orryx/Kether 编译器。
- Station 只确认 event 非空，无法证明 Trigger 已注册。
- UI 不生成图片、模型、声音或客户端脚本资源。
- Combat 不验证客户端实际存在动画，只能检查已读 Controller 声明中的有限关系。
- validator 只覆盖已编码的配置域和规则，不等价于完整服务器启动验证。
- 多文件 materialize 没有事务回滚。

这些缺口必须通过 `requirements`、部署检查、临时服务器预检或人工审核解决。

## 7. 确定性与可复现性

合同层会稳定排序结果数组，YAML dumper 禁用 alias 并固定换行，progression 使用 `Decimal` 计算，`provenance.inputDigest` 来自规范化输入的排序 JSON。service runner 也会按 JSON Pointer 与错误码稳定排序边界错误，不加入时间戳、请求 UUID 或原始异常文本。相同有效输入、相同可信服务上下文与相同可见工作区通常会得到相同 envelope、候选内容和摘要。

但不能把当前实现描述为完全与主机无关：Kether 加载外部 `actions-schema.json` 时，检查消息可能包含解析后的本机路径；workspace 文件集合和内容也会影响结果。需要跨主机 golden snapshot 时，调用方应固定 fixture root、固定 actions schema，并在比较前对非语义路径做明确规范化。
