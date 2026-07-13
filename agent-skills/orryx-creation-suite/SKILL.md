---
name: orryx-creation-suite
description: >-
  Orryx Minecraft 技能插件配置创作套件。用于生成或审查 Orryx 游戏技能、Kether 脚本、职业、经验曲线、法力精力成长、中转站、战斗状态机、控制器、选择器和多客户端 UI；也用于创建完整职业套件、检查跨文件引用和安全落盘。触发词包括 Orryx、技能配置、职业配置、经验曲线、Station、中转站、Status、Controller、Kether、选择器、DragonCore、GermPlugin。
license: MIT
activation: /orryx-creation-suite
metadata:
  author: Orryx contributors
  version: 1.0.0
  created: 2026-07-13
  last_reviewed: 2026-07-13
  review_interval_days: 90
provenance:
  maintainer: Orryx contributors
  version: 1.0.0
  created: 2026-07-13
  source_references:
    - example/Orryx
    - src/main/kotlin/org/gitee/orryx
    - agent-skills/orryx-creation-suite/assets/contracts/actions-schema.json
---
# /orryx-creation-suite — Orryx 配置创作总入口

你是 Orryx 配置工程的总协调者。你的任务不是直接拼接一份孤立 YAML，而是先识别配置 ID、上下文、依赖和运行环境，再把工作路由到合适的组件，最后统一校验所有产物。

## 触发

用户可以直接调用：

```text
/orryx-creation-suite 创建一个完整的雷系长枪职业
/orryx-creation-suite 给拳修设计一条 1-60 级经验曲线
/orryx-creation-suite 检查 plugins/Orryx 中所有悬空引用
```

当请求只涉及一个明确领域时，优先使用对应组件，避免无谓加载整个套件。

## 组件路由

| 用户意图 | 使用组件 |
|---|---|
| 扫描配置、排错、发布前检查 | `/orryx-config-validator-skill` |
| 编写或审查 Kether | `/orryx-kether-authoring-skill` |
| 创建主动、蓄力、指向或被动技能 | `/orryx-ability-authoring-skill` |
| 经验、技能点、法力和精力成长 | `/orryx-progression-curve-skill` |
| 新建职业、调整技能组、设计二转骨架 | `/orryx-job-kit-skill` |
| 事件型被动、护盾、Flag、伤害累计 | `/orryx-station-mechanic-skill` |
| 普攻、格挡、闪避、Status 和 Controller | `/orryx-combat-state-controller-skill` |
| 完整职业或跨组件批量生成 | `/orryx-workflow-orchestrator-skill` |
| 几何目标、流式过滤和选择器预设 | `/orryx-selector-library-skill` |
| Bukkit、DragonCore、GermPlugin、ArcartX UI | `/orryx-ui-adapter-skill` |

## 不可绕过的事实

1. `skills`、`jobs`、`experiences`、`stations`、`status`、`controllers` 的主键通常来自文件名，不来自 `Options.Name`。
2. 相同目录树中重复 basename 会形成冲突，不能依赖扫描顺序覆盖。
3. 被动技能经常需要 Station 或 Status；只有描述的 PASSIVE 不代表机制完成。
4. `status` 与 `controllers` 必须联合维护状态名和动画名。
5. 当前源码的 Aim 请求仅支持 Minecraft 1.12.2；其他版本必须阻断或明确降级。
6. Orryx 全量重载没有事务回滚。生成和验证必须在 staging 中完成，禁止边写边 reload。
7. Station 的 `Async: true` 不代表脚本中的 Bukkit 调用自动安全。

## 跨组件工作流

### 完整职业

严格按以下顺序：

1. 使用 validator 扫描现有 ID、兼容插件和引用图。
2. 使用 Kether 组件和源码生成 Action Schema 预检脚本词汇、上下文与线程。
3. 分配职业、经验、技能、Station、Status、Controller ID。
4. 使用 ability 逐个生成技能。
5. 使用 progression 生成经验与资源成长。
6. 使用 job 生成职业主体、技能清单和非原生二转计划。
7. 对事件型机制使用 station。
8. 对普攻、格挡、闪避使用 combat，再按需要生成 selector 和 UI。
9. 使用 validator 对 staging 做完整发布门禁。
10. 只有 error 为零时才允许显式 materialize；本套件不会自动重载服务器。

### 单技能

1. 读取当前职业、技能 ID 和排序。
2. 明确技能类型、等级、伤害、资源、目标和客户端能力。
3. 使用 Kether 组件生成或审查脚本。
4. 使用 ability 生成 YAML。
5. 如果存在受击、攻击、Flag 或持续监听，转 station 生成配套机制。
6. 最后验证技能、职业补丁、变量和资产引用。

## 统一命令

确定性 happy path：

```bash
py -3 scripts/run_pipeline.py --input request.json --output result.json
```

Linux/macOS 可使用：

```bash
python3 scripts/run_pipeline.py --input request.json --output result.json
```

该命令只生成结果 JSON，不直接覆盖项目文件。落盘必须显式执行 `py -3 scripts/materialize.py --input materialize-request.json --output materialize-result.json`，并默认拒绝覆盖。

## 输出要求

每次工作都必须返回统一合同字段：`summary`、`artifacts`、`diagnostics`、`checks`、`references`、`requirements`、`nextSteps`、`metadata`。其中 Artifact 必须包含相对 `path`、`kind`、`mediaType`、`encoding=utf-8`、完整 `content`、`sha256` 与稳定 `metadata`；未知动画、声音、模型、图标、Trigger 或兼容插件语义必须进入 requirements/nextSteps，不能伪装成已验证能力。

禁止把展示名当作配置 ID，禁止猜测不存在的 Kether Action，禁止复制生产凭据，禁止在校验前写入真实配置。

## 详细资料

- [套件契约](references/suite-contract.md)
- [源码与生产案例依据](references/source-evidence.md)
- [安全与发布流程](references/safety-and-release.md)
