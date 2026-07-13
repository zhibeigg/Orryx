# orryx-station-mechanic-skill

当任务涉及 Orryx `stations/*.yml`、事件驱动被动、伤害前后处理、护盾、Flag 生命周期或累计伤害时加载本技能。

## 执行约束

1. 先区分 `Player Damage`（攻击者）与 `Player Damaged`（受击者）。
2. 先选择 Pre/Post，再决定 Priority、Weight、IgnoreCancelled 与 Async。
3. 同 Event/Priority 内按 Weight 降序解释。
4. 修改/取消伤害只放 Pre；记录已结算伤害放 Post。
5. 任何 Bukkit 实体、世界、事件、背包、伤害或视觉操作默认要求主线程。
6. 输出必须列出 Flag/容器/累计值/冷却的创建、读取和清理。
7. 仅生成 artifacts 与诊断，不重载服务器。

## 工具入口

```text
py -3 scripts/run_pipeline.py --input {input} --output {output}
```

共享运行时输出固定为 `artifacts`、`references`、`requirements`、`diagnostics`、`checks` 五个数组。任何 error 均视为不可 materialize。

详细规则：`references/contract.md`；源码依据：`references/source-evidence.md`。
