---
name: orryx-selector-library-skill
description: >-
  Design and validate Orryx inline and preset selectors. Use for built-in geometry, stream selectors, selectors.yml Actions, positional &vN parameters, container composition, inclusion and exclusion filters, target ordering, and safe selector reuse.
license: MIT
activation:
  command: /orryx-selector-library-skill
  intents:
    - create or review an Orryx selector expression
    - create a selectors.yml preset
    - debug container filters and selector parameters
metadata:
  author: NarraFork
  version: 1.0.0
  created: 2026-07-13
  last_reviewed: 2026-07-13
  review_interval_days: 90
provenance:
  repository: Orryx
  sources:
    - core/selector
    - core/parser/StringParser.kt
    - example/Orryx/selectors.yml
  generated_for: orryx-creation-suite
---
# /orryx-selector-library-skill

设计 Orryx 内联 selector 与 `selectors.yml` 预设。selector 字符串是按顺序执行的容器管线：几何 selector 向 container 添加目标，流式 selector 对当前 container 过滤、排序、变换或截断。

## 语法模型

- 几何入口以 `@name args...` 表示，例如 `@range 3.5`、`@sector 3.5 120 2`。
- 反向流式选择器使用 `!@name`；代码也兼容项目中常见的排除写法，但生成时统一使用文档化形式。
- 多个 entry 按文本顺序执行。先过滤再添加几何目标与先添加再过滤的结果不同。
- selector 结果进入 `container`，可通过 `set`、`merge`、`removeIf`、`for` 或 `they` 传给动作。

## 几何 selector

根据意图选择最小几何：球形 range、扇形 sector、环带 annular、环形点 ring、射线 ray/rayhit、线段 line、圆柱、圆锥、圆台、OBB、视锥、最近目标、视线目标、坐标与偏移、地面点、散射点。必须写清半径、角度、高度/厚度、局部方向、是否含边界以及返回实体还是位置。

不要用大范围 `@range` 模拟可用 sector/OBB/ray 的精确命中；不要假设不同几何的参数顺序一致。

## 流式 selector 与过滤器

流式 selector 对已有 container 操作，常见用途包括：

- `@self`/`!@self`：包含或排除自己；
- `@type`/`!@type`：实体类型过滤，例如排除 `ARMOR_STAND`；
- team、pvp、health、limit、random、sort、offset 等过滤或变换；
- `@their` 等上下文切换必须说明 source/target 语义。

过滤器要放在几何目标之后。需要敌对实体时通常同时排除 self、盔甲架和队友，并明确 PVP 规则。

## selectors.yml 预设

预设以 ID 为键、`Actions` 为脚本。调用方通过 selector action 传入位置参数，预设内使用 `&v0`、`&v1`……读取；索引必须连续、带类型/单位说明，并在缺参时产生 error。示例模式：

```yaml
近战扇形:
  Actions: |-
    set result to container they inline "@sector {{ &v0 }} {{ &v1 }} 2 !@self !@type ARMOR_STAND !@team"
```

预设应返回或构造一个 container，不要把选择与伤害、声音、冷却等业务副作用混在一起。若必须使用 `removeIf`，记录 source 与 damage type 等上下文依赖。

## 工作流

1. 确定目标种类、几何、原点、朝向和边界。
2. 选择几何 selector，并按顺序附加流式过滤器。
3. 若需复用，定义 preset ID、参数表和 `&vN` 映射。
4. 检查 container 的添加/过滤顺序、重复目标、实体/位置类型与空结果。
5. 运行：`py -3 scripts/run_pipeline.py --input assets/request.example.json --output selector-report.json`。
6. 只输出 selector/preset artifact 与验证报告；不执行服务器重载。

## 输出要求

输出固定组件 `selector`，包含规范化表达式或 preset YAML、参数表、container 类型、过滤顺序、错误和警告。未知 selector、缺失参数、非连续 `&vN`、几何参数非法或过滤器先于数据源必须是 error。

## 禁止事项

- 不把几何 selector 与流式 selector 当作可任意换序的集合。
- 不在预设中隐藏未声明的 `&vN`。
- 不盲目扩大范围弥补错误几何。
- 不忘记 self、team、PVP 和 ARMOR_STAND 过滤语义。
- 不把业务伤害副作用塞进通用 selector 预设。
- 不自动重载服务器。

详细字段见 `references/contract.md`，依据见 `references/source-evidence.md`。
