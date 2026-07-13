---
name: orryx-workflow-orchestrator-skill
description: >-
  Orchestrate multi-component Orryx creation in a strict validation-first sequence. Use for dependency graphs, cross-file IDs and references, validator and Kether gates, component dispatch, error-zero materialization, and safe plans that never reload a live server.
license: MIT
activation:
  command: /orryx-workflow-orchestrator-skill
  intents:
    - create a complete Orryx feature across components
    - validate a multi-file Orryx design
    - coordinate suite skills before materialization
metadata:
  author: NarraFork
  version: 1.0.0
  created: 2026-07-13
  last_reviewed: 2026-07-13
  review_interval_days: 90
provenance:
  repository: Orryx
  sources:
    - agent-skills/orryx-creation-suite
    - Orryx resource directory contracts
    - Orryx Kether and reload boundaries
  generated_for: orryx-creation-suite
---
# /orryx-workflow-orchestrator-skill

编排跨技能、职业、中转站、状态机、选择器和 UI 的 Orryx 创建任务。该组件不取代领域组件，而是建立 ID/引用图、按固定顺序调用它们，并以“全部 error 清零”作为 materialize 的唯一入口。

## 固定执行顺序

不得重排以下阶段：

1. `validator`
2. `kether`
3. `ability`
4. `progression`
5. `job`
6. `station`
7. `combat`
8. `selector`
9. `ui`

未涉及的阶段标记为 skipped，但仍保留顺序。任何阶段出现 error，后续阶段可以继续做只读分析以收集错误，但不得 materialize。

## 阶段 0：ID 与引用图

在领域生成前先列出全部定义与引用：skill/job/status/controller/station/selector preset/UI asset/placeholder/animation/flag/variable。每个节点记录类型、ID、计划路径和提供者；每条边记录引用字段与消费者。检测重复 ID、大小写漂移、路径冲突、悬空引用和循环依赖。

## 阶段契约

- **validator**：检查请求 JSON、目标路径、允许组件、命名、重复和危险操作。
- **kether**：解析所有 Actions/Condition/脚本字段，检查变量、selector、引用和线程风险。
- **ability**：技能类型、消耗、冷却、等级变量和动作。
- **progression**：经验、等级、点数及其上限和引用。
- **job**：职业技能列表、状态、初始值与 progression 关系。
- **station**：事件方向、Pre/Post、Priority/Weight、Flag 生命周期和线程。
- **combat**：status/controller 联合、状态时间窗、running 与动画资产。
- **selector**：几何/流式 selector、预设 `&vN` 参数和过滤器。
- **ui**：bukkit/dragoncore/germplugin/arcartx 变体、占位符和资产。

## 错误门

报告必须维护聚合 `errors`。只有同时满足以下条件才允许 `materialized: true`：

- 每个已请求阶段完成；
- 引用图无悬空边与重复定义；
- 所有阶段 `errors` 数量为 0；
- 输出路径位于允许的 Orryx 配置树；
- 用户明确请求 materialize；
- 写入计划不包含服务器重载或命令执行。

warning 不自动阻止 materialize，但必须展示并保留。缺失资产、未知第三方后端能力或无法验证的脚本语义应升级为 error，而不是猜测。

## 工作流

1. 规范化请求，建立组件范围与目标根目录。
2. 建立 ID 与引用图，并先运行 validator。
3. 抽取所有 Kether 字段，通过 kether gate。
4. 严格按固定顺序分派领域组件，合并 artifacts/errors/warnings/references。
5. 再次验证跨组件引用与目标路径。
6. 运行：`py -3 scripts/run_pipeline.py --input assets/request.example.json --output orchestration-report.json`。
7. 默认只生成计划；仅在 error 为零且请求明确时 materialize。

## 服务器边界

本组件永远不运行服务器、不发送 Orryx reload、不重启 Bukkit、不连接生产数据库或 Redis。materialize 仅表示按原子写入计划生成配置文件；实际部署和重载属于人工或独立运维流程。

## 输出要求

输出固定组件 `orchestrator`，包括有序 stages、ID/reference graph、聚合 errors/warnings、artifacts、materialized 和写入计划。即使负例被拒绝，也必须返回结构化报告而非半成品文件。

## 禁止事项

- 不跳过 validator 或 kether 直接生成文件。
- 不在引用图完成前分散创建 ID。
- 不在仍有 error 时 materialize。
- 不把 warning 静默丢弃。
- 不自动重载、重启或操作实时服务器。
- 不把领域未知项用虚构配置补齐。

详细字段见 `references/contract.md`，依据见 `references/source-evidence.md`。
