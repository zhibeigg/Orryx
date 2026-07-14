# Orryx Creation Suite 合同说明

## 1. 机器可读合同

- 输入 Schema：`assets/contracts/component-input.schema.json`
- 输出 Schema：`assets/contracts/component-output.schema.json`
- 私有服务 Envelope Schema：`assets/contracts/service-runner-envelope.schema.json`
- Orchestrator Manifest：`assets/contracts/orchestrator-manifest.json`
- Manifest Schema：`assets/contracts/orchestrator-manifest.schema.json`
- 十个最小输入模板：`assets/templates/*.input.json`

Schema 使用 JSON Schema Draft 2020-12。公开合同面向生产调用方；凡与共享 Runtime 1.1.0 存在差异的字段，都在 Schema 描述、manifest `runtimeCompatibility` 和本文中显式标注。

## 2. 输入合同

根节点必须是 JSON object，标准形状为：

```json
{
  "contractVersion": "1.0",
  "component": "ability",
  "operation": "generate",
  "workspace": {
    "root": ".",
    "mode": "standalone"
  },
  "request": {},
  "policy": {
    "strict": true,
    "overwrite": "deny",
    "network": "deny",
    "minecraftVersion": "1.20.4"
  }
}
```

### 2.1 根字段

| 字段 | 生产合同 | Runtime 1.1.0 真实行为 |
|---|---|---|
| `contractVersion` | 必填，固定 `1.0` | 省略时补 `1.0`；其他值拒绝 |
| `component` | 必填，枚举组件 | 省略时默认 `orchestrator`；未知值拒绝 |
| `operation` | 必填，`generate/validate/plan/materialize` | 省略时默认 `generate`；四种公开值均原生接受 |
| `workspace` | 必填，建议 object | Runtime 也接受路径字符串 |
| `request` | 必填 object | 非 object 拒绝 |
| `policy` | 必填 object | 非 object 拒绝；未知键会保留 |

生产 Schema 比 Runtime 默认值更严格，目的是避免调用方依赖隐式上下文。

### 2.2 operation 兼容层

公开合同要求：

- `generate`：计算候选 artifacts，不写盘。
- `validate`：执行只读检查，不写盘。
- `plan`：表达“只规划、不写盘”。
- `materialize`：显式写入候选 artifacts。

Runtime 原生接受四种公开 operation：

- `generate`、`validate` 与 `plan` 都只返回内存结果，不直接写入 Orryx 配置；
- `plan` 可用于表达“仅规划”，领域 runner 仍按其确定性计算逻辑生成候选与诊断；
- `materialize` 只有通过 CLI 专用子命令，或 orchestrator 最后一步的写盘安全门才会执行；
- orchestrator 的 materialize step 要求顶层同为 `operation=materialize`，且前序不存在 error。

### 2.3 workspace

推荐结构：

```json
{
  "root": "plugins/Orryx",
  "mode": "project"
}
```

- `root`：配置根目录。候选 `artifact.path` 和 materialize 目标都相对此目录。
- `mode`：公开值为 `standalone` 或 `project`。
- Runtime object 默认 `root="."`、`mode="standalone"`。
- Runtime 字符串简写会变成 `root=<字符串>`、`mode="project"`。
- `workspace.path` 是 root 缺失时的兼容别名。

当前 Runtime 不根据 `mode` 改变引用检查规则；缺失的必需引用仍会成为 error。任何“standalone 自动降级 warning”的行为都是未实现语义，应以 `WORKSPACE_MODE_NOT_ENFORCED` 记录。

### 2.4 policy

| 字段 | 默认值 | 当前消费方 | 限制 |
|---|---:|---|---|
| `strict` | `true` | 合同层保留 | 没有统一执行器 |
| `overwrite` | `deny` | materialize | `true`、`allow`、`true`、`overwrite` 会被视为允许覆盖；生产模板只用 `deny/allow` |
| `network` | `deny` | 无 | Runtime 不联网，也不执行网络沙箱 |
| `minecraftVersion` | `""` | ability、kether | 非 1.12.2 Aim 会被拒绝 |
| `plugins` | `[]` | 当前无探测器 | 不能证明插件实际安装 |
| `validateReferences` | `true` | 单组件 dispatch | orchestrator 合并后总会检查必需引用 |
| `createParents` | `true` | materialize | false 时父目录必须已存在 |
| `materialize` | 无 | 当前不消费 | 设为 true 不会自动写盘 |
| `reloadServer` | 无 | 当前不消费 | Runtime 不执行 reload |

## 3. 组件 request 最小语义

| component | 最小生产输入 | 当前候选输出或检查 |
|---|---|---|
| `ability` | 安全 key/id/name、技能 `type`；非 PASSIVE 需 `actions` | `skills/<key>.yml`；被动可附带 Station |
| `job` | key/id/name；通常提供 `skills`、`experience` | `jobs/<key>.yml`；可选二转计划 JSON |
| `progression` | key/id、`minLevel`、`maxLevel`、`curve` | Experience YAML 和 progression 报告 |
| `kether` | `script`、`actions` 或 `scripts` | diagnostics/checks，无 artifact |
| `validator` | workspace；request 可为空 | 工作区 diagnostics/checks |
| `station` | key/id、`event`、`actions` | `stations/<key>.yml` |
| `combat` | `status` 与 `controller` object | Status 与 Controller 两个 YAML |
| `selector` | 非空 `selectors`，或 key+actions 简写 | 完整 `selectors.yml` 候选 |
| `ui` | `backend`；可选 `config` 或 `files` | `ui/<backend>/*.yml` |
| `orchestrator` | 非空 `request.steps` | 合并后的五类数组 |

Schema 只表达稳定的形状边界。诸如 Trigger 是否注册、Kether action 是否由当前服务器提供、动画和图标是否存在，必须由外部证据或 requirement 处理。

## 4. 输出合同

### 4.1 Runtime 核心字段

当前 `finalize_result` 必然返回统一字段：

```json
{
  "contractVersion": "1.0",
  "suiteVersion": "1.1.0",
  "component": "ability",
  "operation": "generate",
  "status": "ok",
  "summary": {},
  "artifacts": [],
  "diagnostics": [],
  "checks": [],
  "references": [],
  "requirements": [],
  "nextSteps": [],
  "metadata": {},
  "provenance": {
    "suiteVersion": "1.1.0",
    "inputDigest": "...",
    "deterministic": true
  }
}
```

`status` 只有 `ok` 和 `invalid`：只要存在 `severity="error"` 的 diagnostic 就是 `invalid`。warning 不改变状态。

### 4.2 摘要、下一步与元数据

- `summary` 给出 artifact/error/warning/check 数量和稳定消息；
- `nextSteps` 根据 error、外部 requirements 和候选 artifacts 确定性推导；
- `metadata` 记录候选文件语义、写盘边界、network policy 与 strict；
- `provenance` 同时保留 `inputDigest` 与兼容字段 `inputSha256`，二者值相同。

这些字段不得包含当前时间、随机 UUID、临时目录或无必要的绝对路径，以保持同输入同工作区下的确定性。

### 4.3 Artifact

生产 Artifact 结构要求：

| 字段 | 含义 |
|---|---|
| `path` | 相对 `workspace.root` 的候选路径，使用 POSIX `/` |
| `kind` | `yaml`、`json`、`markdown`、`text` 或组件自定义类别 |
| `mediaType` | 内容媒体类型 |
| `encoding` | 固定 `utf-8` |
| `content` | 完整候选内容 |
| `sha256` | `content.encode("utf-8")` 的 64 位小写十六进制摘要 |
| `metadata` | component、artifact 类型、backend 等稳定元数据 |

Artifact 的存在不表示文件已创建。只有 materialize 成功返回且 artifact metadata 中出现 `materialized=true`，同时存在 `MATERIALIZE_WRITTEN` pass check，才表示该次调用已写入对应目标。

### 4.4 Diagnostics、Checks、References、Requirements

- `diagnostics`：至少包含 `severity/code/path/pointer/message/suggestion`，可带 `details`。
- `checks`：`code/status/message`，可带 `details`。当前 status 是 runner 自行提供的小写字符串。
- `references`：`source/target/kind/required`。必需引用会由分派层检查候选和工作区文件。
- `requirements`：Runtime 无法自行满足或确认的外部前置条件。它不是 warning 的别名，不会自动让 status 变为 invalid。

结果数组由合同层稳定排序。消费者不能依赖组件执行顺序来推断数组位置，应通过 `path`、`code`、`source/target` 等字段识别项目。

## 5. 私有服务 Envelope

私有 Service Runner 专供 `orryx-edit` AI Job 调用。公开请求是 `envelopeVersion=1.0` 与 `contract` 的二元结构；该 contract 不包含 workspace，只接受 `generate/validate/plan`。`orryx-edit` 服务宿主必须为每个请求启动独立子进程、创建一次性临时 overlay，并在请求结束后销毁两者；不得复用可写生产目录，也不得把 Service Runner 暴露为通用文件执行服务。

可信 `workspace_root`/`workspace_mode` 只能由宿主指向该临时 overlay。官方 Kether Action Schema 必须由可信服务端注入 `actions_schema`；公开请求提供 `actionsSchemaPath` 或任何形式的 `actionsSchema` 都会被拒绝。运行时还会递归拒绝 `materialize` component/operation、`workspace`、覆盖允许、`policy.materialize` 与 `reloadServer`，因此不能通过 orchestrator step 或深层 request 绕过。

服务响应固定包含 `envelopeVersion/status/result/errors`。`completed` 表示边界接受且已得到统一组件结果，组件结果仍可因领域诊断而为 `invalid`；`rejected` 表示服务合同或基础设施边界失败，此时 `result=null`，`errors` 使用 `service-runner-envelope.schema.json` 枚举的稳定 `SERVICE_*` code。成功结果中的 `artifacts`、`diagnostics`、`checks`、`references`、`requirements` 只进入 `orryx-edit` 云草稿，不写回临时 overlay、用户工作区或生产 Orryx 目录。

该入口是附加的窄边界，不改变下面仅供本地受控流程使用的 materialize 合同；私有服务本身永远不能到达该能力。

## 6. materialize 合同

推荐请求：

```json
{
  "contractVersion": "1.0",
  "component": "materialize",
  "operation": "materialize",
  "workspace": {
    "root": "plugins/Orryx-staging",
    "mode": "project"
  },
  "request": {
    "artifacts": []
  },
  "policy": {
    "strict": true,
    "overwrite": "deny",
    "network": "deny",
    "minecraftVersion": "1.20.4",
    "createParents": true
  }
}
```

推荐通过 CLI `materialize` 子命令进入写盘实现。`run` 子命令不会直接分派顶层 `component=materialize`；Orchestrator 也可包含最后的 materialize step，但要求顶层 `operation=materialize` 且前序没有 error。

写盘是逐文件原子替换，不是整批事务。调用方应优先写入独立 staging root，完成磁盘级 validator 和服务器预检后，再执行受控发布。Runtime 不负责备份、跨文件回滚或服务器重载。

## 7. 兼容性 requirements

生产集成至少识别以下 requirement：

- `SERVER_PREFLIGHT_REQUIRED`：静态检查不能替代隔离服务器加载验证。
- `WORKSPACE_MODE_NOT_ENFORCED`：mode 暂不改变行为。
- `NETWORK_POLICY_DECLARATIVE`：network 需由宿主限制。
- `MATERIALIZE_NOT_TRANSACTIONAL`：没有批次回滚。
- `KETHER_SCHEMA_MISSING`：缺 actions schema 时只能做有限检查。
- `PASSIVE_STATION_REQUIRED`：被动效果需要事件入口。
- `COMBAT_BACKEND_PLUGIN_REQUIRED`、`UI_BACKEND_REQUIRED`：外部 backend 前置条件。
- `JOB_ADVANCEMENT_NOT_NATIVE`：二转输出只是实施计划，不是 Orryx 原生字段。
