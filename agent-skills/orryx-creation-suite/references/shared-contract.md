# Orryx Creation Suite 共享合同

本文是十个组件技能共用的简明合同。完整解释见 `../docs/architecture.md`、`../docs/contract.md` 和 `../docs/workflows.md`；机器可读版本见 `../assets/contracts/`。

## 1. 版本与 Runtime

- `contractVersion`：`1.0`
- `suiteVersion`：`1.1.0`
- 共享实现：`shared/orryx_toolkit`
- 组件 wrapper 必须复用共享 Runtime，不应复制领域逻辑形成分叉。

## 2. 标准输入

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

生产调用方必须显式提供六个根字段。Runtime 虽有默认值，但默认值不能替代审计信息。

公开 operation 为：

- `generate`：计算候选内容；
- `validate`：只读检查；
- `plan`：只规划；
- `materialize`：显式写盘。

Runtime 1.1.0 原生接受 `generate/validate/plan/materialize`。其中前三者只计算或校验；`materialize` 受显式 CLI/Orchestrator 安全门约束。

## 3. workspace 与 policy

- `workspace.root` 是读取和 materialize 的边界。
- `workspace.mode` 公开值为 `standalone/project`，但当前 Runtime 不据此改变引用诊断。
- 字符串 workspace 是兼容简写，不是推荐生产格式。
- `policy.overwrite` 默认 `deny`。
- `policy.validateReferences` 默认 `true`；单组件可关闭，orchestrator 合并后仍检查必需引用。
- `policy.createParents` 默认 `true`。
- `policy.strict` 和 `policy.network` 当前主要是声明值；Runtime 没有统一严格模式或网络沙箱。
- `policy.minecraftVersion` 会参与 Aim 兼容检查。
- 任何 `materialize=true` 或 `reloadServer=true` 意图字段都不会自动执行写盘或重载。

## 4. 标准输出

Runtime 统一输出必须包含：

- `contractVersion`、`suiteVersion`、`component`、`operation`、`status`
- `summary`
- `artifacts`
- `diagnostics`
- `checks`
- `references`
- `requirements`
- `nextSteps`
- `metadata`
- `provenance`

这些字段均由 `finalize_result` 原生、确定性生成。

`status=invalid` 当且仅当 diagnostics 中至少存在一个 `severity=error`；warning 和 requirement 不自动改变 status。

## 5. Artifact 规则

每个生产 Artifact 包含：

- `path`
- `kind`
- `mediaType`
- `encoding=utf-8`
- `content`
- `sha256`
- `metadata`

`artifacts` 永远先视为候选。它们不会因为 `generate`、`validate`、`plan` 或 `policy.materialize=true` 自动写盘。

CLI `materialize`，以及满足“顶层 operation=materialize 且前序零 error”的 orchestrator 最后一步可以写文件。写盘会验证摘要、相对路径、root 边界、重复目标、覆盖策略和父目录策略，然后逐文件执行同目录临时文件加 `os.replace`。该机制只有单文件原子性，没有跨文件事务；中途失败可能留下前序已写文件。

Runtime 不执行 Orryx reload、服务器命令或重启。

## 6. 引用与 requirement

- required reference 必须由同一 bundle 的 artifact path 或 workspace 中的实际文件满足。
- optional reference 不会阻断。
- `requirements` 用于表达 Runtime 无法证明或无法实现的前置条件，例如 Trigger 注册、actions schema、客户端资源、第三方 backend、非原生二转和部署步骤。
- 不得把未知语义写成已验证能力。

关键 requirements：

- `SERVER_PREFLIGHT_REQUIRED`
- `WORKSPACE_MODE_NOT_ENFORCED`
- `NETWORK_POLICY_DECLARATIVE`
- `MATERIALIZE_NOT_TRANSACTIONAL`
- `KETHER_SCHEMA_MISSING`
- `PASSIVE_STATION_REQUIRED`
- `JOB_ADVANCEMENT_NOT_NATIVE`
- `COMBAT_BACKEND_PLUGIN_REQUIRED`
- `UI_BACKEND_REQUIRED`

## 7. Orchestrator

推荐依赖顺序：

```text
validator → kether → ability → progression → job → station → combat → selector → ui → materialize
```

当前 orchestrator：

- 按固定依赖顺序重排，且保持同组件 step 的输入相对顺序；
- 不允许递归 orchestrator；
- 接受受控 materialize 最后一步；
- 合并五类共享数组；
- 检查重复 artifact path；
- 检查 required references；
- 前序有 error 时跳过 materialize。

`generate/validate/plan` 不写盘；materialize step 的安全门由 Runtime 保证。

## 8. 组件边界

- Validator 是有限静态检查，不是服务器加载证明。
- Kether 是轻量扫描器，不是编译器。
- Passive Ability 通常需要 Station 或其他事件入口。
- Aim 当前仅支持 Minecraft `1.12.2`。
- Job advancement 只生成实施计划，不是原生 ParentJob。
- Status 与 Controller 必须联合维护。
- Selector 输出整份 `selectors.yml`，现有文件合并需要调用方完成。
- UI 不生成图片、模型、声音或客户端资产。

## 9. Golden/Eval

每个组件目前有 eval Markdown、通用 runner 和至少三个 golden input。`--validate` 只验证 spec；`--rollout` 才执行真实 pipeline；`llm-judge` 不自动评分。现有 pending-first-green case 不能被描述为已有稳定基线，runner 也不会自动比较 expected 与 produced。建立生产回归门禁时必须增加明确的路径、摘要、诊断和内容断言。

## 10. `orryx-edit` 私有 Service Runner

- 私有 Service Runner 只供 `orryx-edit` AI Job 调用，每个请求必须运行在独立子进程与一次性临时 overlay 中，请求结束后销毁。
- 公开 service contract 只允许 `generate/validate/plan`，递归拒绝 `materialize`、`actionsSchemaPath`、任何 `actionsSchema`、`workspace`、允许覆盖的 `overwrite`、`policy.materialize` 与 `reloadServer`。
- 临时 overlay 只能由可信宿主注入为 workspace；官方 Kether Action Schema 只能由可信服务端注入，不能由 AI Job 指定路径或内容。
- `artifacts`、`diagnostics`、`checks`、`references`、`requirements` 只进入 `orryx-edit` 云草稿；Service Runner 不写 overlay、用户工作区或生产 Orryx 目录。
- 该服务不新增网络客户端、shell、materialize 或可写生产目录能力；本地 CLI materialize 合同与服务边界严格分离。
