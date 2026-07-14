# 安全与发布流程

## 阶段

1. `scan`：只读扫描现有配置和 ID。
2. `generate`：在内存中生成 artifact。
3. `validate`：执行结构、Kether、引用、兼容与安全检查。
4. `preview`：展示路径、内容摘要、资源要求和诊断。
5. `materialize`：本地用户明确要求后通过 CLI，或通过顶层 `operation=materialize` 且前序零 error 的本地 Orchestrator 最后一步写入 staging/目标目录。私有 service runner 不提供此阶段。
6. `runtime preflight`：可选，在临时服务端验证加载。
7. `reload`：不由本套件自动执行。

## 私有服务合同边界

私有服务必须调用 `shared/orryx_toolkit/service_runner.py`，公开请求/响应遵循 `assets/contracts/service-runner-envelope.schema.json`。公开 contract 只允许 `generate`、`validate`、`plan`，且不得携带 workspace；`workspace_root`、`workspace_mode` 与可信 Action Schema 由服务宿主作为独立关键字参数注入。

入口会递归拒绝所有已知写盘和路径注入绕过：顶层或 step 的 materialize component/operation、未知 operation、任何 `workspace`、`actionsSchemaPath`、任何形式的 Action Schema、`policy.materialize`、允许覆盖的 overwrite 值、`network!=deny`、`strict!=true`，以及任何深度的 `reloadServer`。拒绝结果固定为 `status=rejected`、`result=null` 和稳定排序的 `SERVICE_*` errors；接受结果为 `status=completed`，其中领域合同自身仍可返回 `result.status=invalid`。

这条服务边界不会删除或改变本地 CLI/materialize 能力。不得把本地 `run_contract` 或 `execute("materialize", ...)` 直接暴露给不可信服务请求。

## 路径规则

- Artifact 路径必须是相对路径。
- 禁止 `..`、绝对路径、盘符和 UNC 路径。
- 规范化后必须仍位于指定 root。
- 默认拒绝覆盖。
- 更新现有文件前应保留 diff 或备份。

## 秘密值

不得从生产配置复制：

- 数据库密码。
- Redis 密码。
- API Key、Token、Webhook。
- 编辑器认证信息。
- 第三方平台密钥。

模板只允许环境变量名、空字符串或明确的非秘密示例值。

## Bukkit 线程

Station `Async: true` 只决定脚本调度位置，不保证以下操作安全：

- 实体和世界读写。
- 玩家物品栏和状态修改。
- Bukkit 事件操作。
- 客户端插件 API。

涉及这些动作时应使用同步边界或把 Station 保持为同步。数据库与 Redis I/O 则必须异步。

## 发布阻断条件

出现以下任一情况不得 materialize 到生产目录：

- YAML 解析失败。
- 重复文件 ID。
- 必需跨文件引用不存在。
- 主动技能缺失 Actions。
- Aim 类型与目标版本不兼容。
- Station Event 不存在或 Priority 非法。
- 状态区间结构错误。
- Kether 确认编译失败。
- 检测到可能的秘密值。
- Artifact 路径逃逸。
