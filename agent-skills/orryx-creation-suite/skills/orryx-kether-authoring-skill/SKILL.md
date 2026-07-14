---
name: orryx-kether-authoring-skill
description: >-
  Author and review Orryx Kether Actions for skill, station, state, job, and experience contexts. Use when a request needs context-aware variables, selectors, triggers, event access, deterministic script artifacts, or main-thread safety warnings. Treats the generated Actions Schema as an action vocabulary rather than a complete YAML schema.
license: MIT
activation:
  command: /orryx-kether-authoring-skill
  intents:
    - write Orryx Kether actions
    - review Kether context
    - check Kether thread safety
metadata:
  author: NarraFork
  version: 1.1.0
  created: 2025-07-13
  last_reviewed: 2025-07-13
  review_interval_days: 90
provenance:
  repository: Orryx
  evidence: references/source-evidence.md
  contract: references/contract.md
---
# /orryx-kether-authoring-skill

编写或审查 Orryx Kether `Actions`，先确定宿主上下文，再选择可用参数与动作。

## 上下文必须显式区分

| 上下文 | 典型宿主 | 可依赖事实 |
|---|---|---|
| `skill` | 技能 `Actions`、扩展动作 | `SkillParameter`，技能 ID、等级、触发器、原点与技能变量 |
| `station` | Station `Actions` | `StationParameter`，Station ID、sender、event；事件动作只在此处合理 |
| `state` | Status/State Action | `StateParameter` 或 Status 脚本状态；输入与运行状态语义不同于技能 |
| `job` | 职业数值动作 | 玩家、职业、等级；用于法力、精力、升级点等表达式 |
| `experience` | `ExperienceOfLevel` | sender 与 `level`，目标是逐级返回整数经验 |

不要把 `&event`、`&pressTick`、技能变量或状态输入跨上下文搬用。

## Actions Schema 的边界

`actions-schema.json` 描述 Action、Selector、Trigger、Property、类型、语法、示例、执行与插件要求。它不是完整 Orryx YAML Schema，也不能证明：

- YAML 顶层结构正确；
- 当前宿主上下文提供某个变量；
- 跨文件引用存在；
- 动作组合在目标线程安全。

因此 Schema 用于词汇与语法证据，宿主契约仍需单独校验。

## 主线程规则

Bukkit 世界、实体、背包、药水、速度、注册监听器等操作按主线程敏感处理。若脚本可能从异步 Station 或协程执行，输出 warning，并要求用 Orryx 的 `sync { ... }` 或对应 ensure-sync 动作包裹最小必要区段。不要建议阻塞等待、`join()`、`get()` 或睡眠线程。

## 工作流

1. 从请求读取 `context`、目标效果、可用变量、服务器版本和插件。
2. 拒绝或诊断上下文不匹配的变量/动作。
3. 依据 Actions Schema 选择语法，但不把它冒充完整 YAML 验证。
4. 将主线程敏感段缩小并显式同步。
5. 返回脚本 artifact、requirements、references、checks 与稳定诊断。

## 运行

```powershell
py -3 scripts/run_pipeline.py --input assets/request.example.json --output result.json
```

## 验收

- 输出 component 固定为 `kether`。
- 结果说明上下文假设。
- 外部插件动作进入 `requirements`。
- 负例返回合法 `status: invalid` JSON，而不是进程崩溃。
