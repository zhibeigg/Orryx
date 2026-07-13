# orryx-ui-adapter-skill

当任务涉及 Orryx UI/HUD、Bukkit Inventory、DragonCore、GermPlugin 或 ArcartX 配置与适配时加载本技能。

## 执行约束

1. 请求必须选择唯一 `variant`: bukkit/dragoncore/germplugin/arcartx。
2. 每个 variant 使用独立 schema，不混合后端字段。
3. 技能、按键、图标、冷却和占位符数组保持索引顺序与长度一致。
4. 图标、GUI、图片、字体、声音和客户端文件均作为资产依赖验证。
5. 跨后端先做能力差异表；不可保真项不得盲目转换。
6. 缺少 variant、数组错位、未知 placeholder 或必需资产时返回 error。
7. 不安装第三方插件，不重载服务器。

## 工具入口

```text
py -3 scripts/run_pipeline.py --input {input} --output {output}
```

详细规则：`references/contract.md`；源码依据：`references/source-evidence.md`。
