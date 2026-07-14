---
name: orryx-ability-authoring-skill
description: >-
  Design deterministic Orryx ability YAML for Passive, Direct, DirectAim, Pressing, and PressingAim skills. Use when creating or reviewing ability type fields, Description/Variables/Actions consistency, passive Station companions, charging behavior, targeting compatibility, and cross-file requirements based on real Orryx examples.
license: MIT
activation:
  command: /orryx-ability-authoring-skill
  intents:
    - create Orryx ability
    - review Orryx skill YAML
    - design passive or charging skill
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
# /orryx-ability-authoring-skill

生成 Orryx 技能定义与必要的配套 scaffold，不写入真实工作区。

## 五种技能类型

- `Passive`：只有元数据，不由技能释放器执行 `Actions`；实际被动效果常需要 Station 监听事件。
- `Direct`：立即施放。
- `DirectAim`：先由客户端选择目标位置，再施放。
- `Pressing`：周期蓄力，释放时执行主 `Actions`。
- `PressingAim`：蓄力与客户端指向结合。

`DirectAim` 与 `PressingAim` 当前客户端瞄准实现仅支持 Minecraft `1.12.2`。其他版本必须诊断为不兼容，不能静默降级。

## 一致性要求

1. 文件 basename 是技能 ID；`Options.Name` 是显示名。
2. `Description` 中展示的冷却、消耗、伤害、阶段和升级条件必须能追溯到 `Variables` 或动作。
3. `Variables` 键在运行时转为大写索引，但案例通常用大小写不敏感的脚本访问；不要制造同名异形键。
4. `Actions` 使用的变量必须定义或来自明确上下文。
5. Pressing 类型必须给出周期、最大蓄力时间与释放阶段逻辑。
6. Passive 若声明事件驱动效果，生成 Station scaffold/reference；不要虚构 Passive 自己会监听事件。

## 真实模式

- `破空斩`：Direct；描述、固定伤害、倍率变量与 `Actions` 对应，并把 Bukkit 敏感动作放入 `sync`。
- `蓄意轰拳`：Pressing；`Period`、`PressPeriodAction`、`MaxPressTickAction`、阶段阈值与最终倍率一致。
- `内劲` + `拳修内劲`：Passive 技能作为展示与拥有关系，Station 承担 `Player Damage Pre` 事件效果。

## 工作流

1. 明确技能 ID、类型、版本、插件依赖和数值规格。
2. 建立 Description → Variables → Actions 可追踪矩阵。
3. 生成技能 artifact；必要时生成 Station scaffold artifact。
4. 添加跨引用与版本诊断。
5. 保持输出确定性，不包含当前时间或绝对路径。

## 运行

```powershell
py -3 scripts/run_pipeline.py --input assets/request.example.json --output result.json
```

## 禁止事项

- 不把 `Options.Name` 当成文件 ID。
- 不为被动效果伪造自动执行的 `Actions`。
- 不在非 1.12.2 上宣称 Aim 可用。
- 不让 Description 数值与实际变量/动作脱节。
