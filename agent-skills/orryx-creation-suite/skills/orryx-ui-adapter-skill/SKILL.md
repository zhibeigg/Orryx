---
name: orryx-ui-adapter-skill
description: >-
  Design and validate Orryx UI configuration as explicit bukkit, dragoncore, germplugin, or arcartx variants. Use for backend-specific schemas, placeholders, ordered arrays, skill icons, GUI and client assets, capability gaps, and safe non-lossy adaptation.
license: MIT
activation:
  command: /orryx-ui-adapter-skill
  intents:
    - create or review an Orryx UI configuration
    - adapt a UI to a specific supported backend
    - validate placeholders, slots, icons, and UI assets
metadata:
  author: NarraFork
  version: 1.0.0
  created: 2026-07-13
  last_reviewed: 2026-07-13
  review_interval_days: 90
provenance:
  repository: Orryx
  sources:
    - module/ui
    - src/main/resources/ui
    - example/Orryx/ui
  generated_for: orryx-creation-suite
---
# /orryx-ui-adapter-skill

为 Orryx UI 生成或审查明确的判别联合（discriminated variant）。输入必须选择 `bukkit`、`dragoncore`、`germplugin` 或 `arcartx`；每个后端有独立 schema、占位符传输、数组顺序和客户端资产契约，不能把它们当作同一份 YAML 的皮肤。

## 判别字段

所有请求先规范化为 `variant`：

- `bukkit`：原版 Inventory UI，核心是 title、Skills/BindSkills/Space/Previous/Next 的 Slots、物品材质、名称与 Lore。
- `dragoncore`：依赖 DragonCore GUI 文件、同步占位符、`<br>` 编码的有序数组、图标路径和客户端资源。
- `germplugin`：依赖 Germ GUI part/index 名、动态复制的按钮/纹理、`{skill}` 图标替换、布局间距和事件回调。
- `arcartx`：依赖 ArcartX UI/网络变量结构、icon 字段和对应客户端界面资产。

`UI.use` 的别名可以归一化（DRAGON、GERM、AX），但输出 variant 使用四个规范值之一。

## 共通契约

- `keyTime`、`KeySort`、`ActionType`、`JoinOpenHud` 只有在后端实现支持时输出。
- 技能列表、绑定键、图标、冷却数组必须维持同一顺序；不能用无序 map 重新排列。
- 占位符名称、分隔符和数组长度是协议的一部分。DragonCore 的 `<br>` 列表必须逐索引对齐。
- `skill.getIcon()` 的结果是后端资源引用，不保证是 Bukkit Material；每个 variant 都要验证图标命名空间与资产是否存在。
- UI/HUD 模板、图片、字体、声音、GUI 文件和客户端插件版本都作为显式依赖输出。

## 适配策略

适配不是字段替换：

1. 先读取源 variant 的语义能力和目标 variant 的能力。
2. 建立保真映射、降级项和不可转换项。
3. 对不可表达的布局、动态事件、动画或占位符返回 error 或要求独立目标设计。
4. 只有等价语义才自动迁移；否则保留源配置并生成目标端设计说明。

例如 Bukkit slot 数组不能自动推导 Germ 动态 canvas 布局；DragonCore `<br>` placeholder 不能盲目变成 ArcartX map；Germ part 名也不能假设存在于 DragonCore GUI。

## 工作流

1. 选择唯一 variant，并收集 UI/HUD 名称、技能顺序、绑定键、图标与资产清单。
2. 加载该 variant 的 schema，不混入其他后端字段。
3. 验证数组长度与索引对齐、placeholder、图标路径、GUI/客户端资产。
4. 若为适配任务，生成能力差异表与不可转换项。
5. 运行：`py -3 scripts/run_pipeline.py --input assets/request.example.json --output ui-report.json`。
6. 只输出目标 variant artifacts 和依赖报告；不安装第三方插件，不重载服务器。

## 输出要求

输出固定组件 `ui`，包含规范 variant、后端专属 artifacts、placeholder/array contract、asset dependencies、errors、warnings 和转换损失。缺少 variant、混合后端字段、数组错位、未知 placeholder 或缺失必需资产必须是 error。

## 禁止事项

- 不生成没有判别 variant 的“通用 UI YAML”。
- 不盲目互转四种后端。
- 不重排技能、绑定键、图标或冷却数组。
- 不把服务端图标名假设为客户端已存在资产。
- 不虚构 GUI part、placeholder、图片或真实密钥。
- 不自动安装插件或重载服务器。

详细字段见 `references/contract.md`，依据见 `references/source-evidence.md`。
