# Agent instructions

- 先确定 `skill`、`station`、`state`、`job` 或 `experience` 上下文，再写动作。
- Actions Schema 只作为动作/选择器/触发器/属性词汇证据，不把它当完整 YAML Schema。
- `&event` 仅在 Station 事件上下文使用；技能等级/变量与状态输入不得跨上下文臆用。
- Bukkit 世界、实体、物品栏、药水与监听器操作标记为主线程敏感；建议最小 `sync {}` 区段。
- 不建议阻塞调用或线程睡眠。
- 外部插件动作必须进入 requirements；不具备插件时返回诊断。
- 通过共享 `orryx_toolkit` 运行，不在组件中复制实现。
