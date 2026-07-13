# orryx-workflow-orchestrator-skill

当一个 Orryx 请求跨越多个配置组件时加载本技能。它负责依赖图、固定阶段顺序和 materialize 错误门，不替代领域技能。

## 固定阶段

`validator → kether → ability → progression → job → station → combat → selector → ui`

## 执行约束

1. 首先建立全部 ID 与引用图。
2. 未涉及阶段可标记 skipped，但不得重排。
3. 各阶段只追加共享五数组输出并保留来源。
4. 所有 diagnostics error、悬空引用、重复 ID、非法路径清零后，且请求明确 materialize，才允许生成写入计划。
5. 负例返回结构化 invalid 报告，不产生可写 artifacts。
6. 永不运行 Orryx reload、Bukkit restart 或实时服务器命令。

## 工具入口

```text
py -3 scripts/run_pipeline.py --input {input} --output {output}
```

详细规则：`references/contract.md`；源码依据：`references/source-evidence.md`。
