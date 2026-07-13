# orryx-combat-state-controller-skill

当任务涉及 Orryx `status/*.yml` 与 `controllers/*.yml`、普攻连段、格挡、闪避、动画时间窗或 `running` 路由时加载本技能。

## 执行约束

1. status 与 controller 必须联合设计和验证。
2. 先建 status/state/controller/animation ID 引用图。
3. `Connection`、`Check`、`Invincible` 必须满足非负、有序且不超过 Duration。
4. 所有 `running "state"` 必须指向已定义 State。
5. 普攻、格挡、闪避分别检查输入、资源、动作、成功分支和客户端动画。
6. DragonCore、ArcartX 等控制器资产不盲目互转；缺失资产列为 error/依赖。
7. 不重载服务器。

## 工具入口

```text
py -3 scripts/run_pipeline.py --input {input} --output {output}
```

输出使用共享五数组契约；error 清零前不得 materialize。

详细规则：`references/contract.md`；源码依据：`references/source-evidence.md`。
