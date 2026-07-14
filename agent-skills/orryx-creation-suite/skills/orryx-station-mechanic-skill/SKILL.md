---
name: orryx-station-mechanic-skill
description: >-
  Design and review Orryx station YAML mechanics for event-driven combat and passive behavior. Use for Event, Weight, Priority, Async, Actions, damage Pre/Post direction, shields, flags, cooldown handoff, accumulated damage, and Bukkit thread-safety checks.
license: MIT
activation:
  command: /orryx-station-mechanic-skill
  intents:
    - create or review an Orryx station
    - wire attack or damaged events
    - implement passive, shield, flag, or accumulated-damage mechanics
metadata:
  author: NarraFork
  version: 1.1.0
  created: 2026-07-13
  last_reviewed: 2026-07-13
  review_interval_days: 90
provenance:
  repository: Orryx
  sources:
    - core/station/stations/StationLoader.kt
    - core/station/stations/StationLoaderManager.kt
    - example/Orryx/stations
  generated_for: orryx-creation-suite
---
# /orryx-station-mechanic-skill

创建或审查 Orryx `stations/*.yml` 中转站。目标是把事件方向、执行阶段、权重、线程和 Kether 副作用写成可验证的配置，而不是仅拼接一段 `Actions`。

## 输入

优先读取用户提供的事件名、触发方向、阶段、职业/技能 ID、Flag、伤害类型、依赖资源和现有 YAML。信息不足时采用最保守的同步、`NORMAL` 优先级和显式方向，并在报告中列出假设。

## 必须建立的语义

1. **事件方向**：`Player Damage *` 以攻击者为脚本主体；`Player Damaged *` 以受击者为主体。攻击加成使用前者，护盾减伤使用后者。
2. **阶段**：`Pre` 用于改伤、取消事件和预判；`Post` 用于记录已结算伤害、收集目标或后续反馈。不要在 Post 假装撤销已发生的伤害。
3. **执行顺序**：同一 Event 与 Priority 内，`Weight` 越大越先执行；`Priority` 必须为 Bukkit 的 `LOWEST/LOW/NORMAL/HIGH/HIGHEST/MONITOR`。
4. **脚本契约**：顶层必须有 `Options.Event` 和 `Actions`。按需加入 `BaffleAction`、`IgnoreCancelled`、`Variables`、`Async`。
5. **取消状态**：读取伤害结果的逻辑先判断 `&isCancelled`；需要取消时在 Pre 使用 `event cancelled true`。

## 机制模式

### Passive（被动）与攻击修正

参考“拳修内劲”：监听 `Player Damage Pre`，读取职业后修改 `&event[damage]`。加成公式必须说明是加法、乘法还是固定段，避免把最大生命等单位混入未标注比例。

### 护盾与受击修正

参考“凝灵盾受击”：监听 `Player Damaged Pre`。护盾足够时取消事件并扣盾；不足时只扣除剩余盾值；破盾分支只执行一次，并明确 Flag 的 `remove`、`timeout` 与零值语义。反震范围必须排除 `@self`、盔甲架和队友。

### Flag 生命周期与冷却交接

参考“刹那冷却”：监听 Flag Change Post，在 `newFlag == null` 时设置技能冷却，确保持续态结束后才进入冷却。不要在创建 Flag 时提前结算。

### 累计伤害

参考“咒恶之锋”：监听 `Player Damage Post`，跳过取消事件，将 defender 合入容器并累加已结算 `&event[damage]`。容器、累计值与主 Flag 必须有一致的创建、读取和清理周期。

## Bukkit 线程安全

`Options.Async: true` 会把站点脚本放入插件协程作用域；它不等于所有 Bukkit API 都线程安全。涉及事件取消/改写、实体/世界、背包、声音、粒子、移动、伤害调用或 Bukkit 对象访问时保持同步。只有确认整个动作链为异步安全的纯计算或异步 I/O 时才启用 Async；数据库与 Redis I/O 由对应异步 API 处理，不能阻塞事件线程。

## 工作流

1. 解析目标机制并确定攻击者/受击者方向。
2. 选择 Pre 或 Post，再确定 Priority、Weight、IgnoreCancelled 与 Async。
3. 建立 Flag、容器、累计值和冷却的生命周期表。
4. 编写 `Options` 与 `Actions`，保留 Orryx Kether 语法和事件字段。
5. 检查事件阶段是否允许该副作用，检查线程风险和重复触发。
6. 运行：`py -3 scripts/run_pipeline.py --input assets/request.example.json --output station-report.json`。
7. 仅输出建议文件与验证报告；除非上层编排器明确进入 materialize 阶段，否则不写入服务器配置，也不重载服务器。

## 输出要求

输出应包含固定组件 `station`、状态、错误与警告、事件方向、阶段判断、线程风险、引用 ID，以及建议的 YAML artifact。任何 error 都必须阻止 materialize。

## 禁止事项

- 不把 `Player Damage` 与 `Player Damaged` 当同义词。
- 不在 Post 阶段修改或取消已结算伤害。
- 不在异步站点中直接调用主线程敏感 Bukkit 操作。
- 不忽略取消状态后继续累计伤害。
- 不生成不存在的事件字段、动作或真实密钥。
- 不自动执行 Orryx/服务器重载。

详细字段见 `references/contract.md`，依据见 `references/source-evidence.md`。
