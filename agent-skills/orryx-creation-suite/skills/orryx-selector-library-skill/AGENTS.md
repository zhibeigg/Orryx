# orryx-selector-library-skill

当任务涉及 Orryx 内联 selector、`selectors.yml` 预设、`&vN` 参数或 container 过滤链时加载本技能。

## 执行约束

1. 区分几何 selector（添加目标）和流式 selector（处理已有 container）。
2. 按文本顺序解释 entry；过滤器必须位于目标来源之后。
3. 明确原点、方向、几何参数、实体/位置结果与边界。
4. 预设参数 `&v0..&vN` 必须连续、声明类型和单位，不得隐藏缺参。
5. 敌对实体选择显式处理 self、team、PVP、ARMOR_STAND。
6. 通用预设不混入伤害、冷却或声音副作用。
7. 不重载服务器。

## 工具入口

```text
py -3 scripts/run_pipeline.py --input {input} --output {output}
```

详细规则：`references/contract.md`；源码依据：`references/source-evidence.md`。
