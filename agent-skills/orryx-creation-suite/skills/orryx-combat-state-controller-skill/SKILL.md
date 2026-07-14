---
name: orryx-combat-state-controller-skill
description: >-
  Build and review Orryx combat status plus client controller pairs. Use for Condition, States, running transitions, Connection, Check and Invincible intervals, Animation mappings, general attacks, blocks, dodges, attack speed, and controller asset consistency.
license: MIT
activation:
  command: /orryx-combat-state-controller-skill
  intents:
    - create an Orryx status and controller pair
    - design attack, block, dodge, or combo states
    - validate combat animation timing and transitions
metadata:
  author: NarraFork
  version: 1.1.0
  created: 2026-07-13
  last_reviewed: 2026-07-13
  review_interval_days: 90
provenance:
  repository: Orryx
  sources:
    - module/state
    - example/Orryx/status/剑修.yml
    - example/Orryx/controllers/长剑.yml
  generated_for: orryx-creation-suite
---
# /orryx-combat-state-controller-skill

联合设计 Orryx `status/*.yml` 与 `controllers/*.yml`。两者是同一个战斗契约：status 决定输入、状态、时间窗与 Kether；controller 提供客户端动画层、动画名和触发行为。

## 核心不变量

1. `Options.Condition` 决定状态集何时生效；武器、职业或场景条件必须明确。
2. `Options.Controller` 必须引用存在的控制器 ID；status 中所有动画 Key/方向动画必须能在对应控制器或客户端资产中解析。
3. `States` 的键是 `running "状态名"` 的引用目标。所有引用先建 ID 图，禁止悬空。
4. Tick 区间使用包含式 `start-end` 表达，并满足 `0 <= start <= end <= Duration`；没有 Connection 就不要宣称可以连段。
5. `Action` 是输入路由；状态自身的 `Action`、`BlockAction` 分别承担进入/命中格挡后的逻辑。

## 状态类型

### General Attack

配置 `Type: General Attack`、`Connection`、`Animation.Key`、`Animation.Duration`。普攻链应有确定的回环或终止分支，例如 0→1→2→0。`AttackSpeed` 在状态开始时求值，动作中的 sleep 与客户端动画速度必须共同审查。

### Block

配置 `Check` 检测区间、成功后的 `Invincible` Tick、允许的 `DamageType`、普通与成功动画。`BlockAction` 只在成功格挡时执行；资源消耗可放进入 Action，但必须明确失败是否返还。`Check` 不能超过动画时长。

### Dodge

配置 `Invincible` 区间、可选 `Connection`、`Spirit`、四方向 Animation 与 Duration。方向移动由 `state move` 分派；无敌区间不能超过动作时长，翻滚与闪避的衔接规则必须显式。

## status 与 controller 联合检查

- status 参考 `status/剑修.yml`：`Condition`、`CancelHeldEventWhenPlaying`、`CancelBukkitAttack`、`AttackSpeed`、状态定义和输入路由构成服务端状态机。
- controller 参考 `controllers/长剑.yml`：动画按 Layer 分组，Trigger 控制 idle、walk、sprint、sneak、animationStart/End 等客户端行为。
- controller 的 layer/trigger 是平台资产语义，不能仅凭 status 自动臆造；缺少动画、音效、模型或粒子资产时必须列为依赖。
- DragonCore、ArcartX 或其他控制器格式不保证字段可互换；输出必须声明目标后端。

## 工作流

1. 收集武器/职业条件、后端、输入键、动画资产和资源消耗。
2. 建立 status ID、state ID、controller ID、animation ID 与 `running` 引用图。
3. 为每个状态建立 Duration、Connection、Check、Invincible 时间轴。
4. 设计普攻/格挡/闪避路由，检查不可达状态、死循环和冲突输入。
5. 对照 controller 动画层与资产依赖，保留数组顺序和动作命名。
6. 运行：`py -3 scripts/run_pipeline.py --input assets/request.example.json --output combat-report.json`。
7. error 清零前只输出计划与 artifacts，不 materialize，不重载服务器。

## 输出要求

输出固定组件 `combat`，包含 status/controller 两个 artifact、引用图、每个状态的时间轴、资产依赖、错误和警告。任何悬空 `running`、非法区间或缺少 controller 引用都必须是 error。

## 禁止事项

- 不只生成 status 而忽略 controller 契约，反之亦然。
- 不让 Connection、Check 或 Invincible 超出 Duration。
- 不把 `running` 目标写成未定义状态。
- 不假设四方向动画、成功格挡动画或资源文件一定存在。
- 不盲目把 DragonCore 控制器转换为其他后端。
- 不自动重载服务器。

详细字段见 `references/contract.md`，依据见 `references/source-evidence.md`。
