# Kether component contract

## Request

launcher 固定注入 `component: kether`。`request` 至少提供 `script`、`actions` 或 `scripts` 之一，并建议描述：

- `context`: `ability | station | state | job | experience | generic`
- `variables`: 宿主明确提供的变量
- 可选 `actionsSchema` 或 `actionsSchemaPath`
- 可选 `minecraftVersion` 与 `skillType`

operation 可为 `generate`、`validate` 或只读 `plan`；三者都不会写盘。未显式提供 Schema 时会回退到工作区或随 Runtime 打包、由 Kotlin 注册点生成的 `actions-schema.json`。

## Context rules

- skill：SkillParameter、技能等级、触发器、原点和技能变量。
- station：StationParameter、sender、event；事件读写动作需此上下文。
- state：StateParameter/Status 运行状态与 input。
- job：职业等级与资源表达式。
- experience：`level` 与 sender，只返回逐级经验值。

## Result

该组件返回 diagnostics/checks，不生成 Kether 文件 artifact；领域组件负责把已检查脚本嵌入 YAML。主线程敏感操作、未知 action、隐式变量和 Aim 版本风险通过 diagnostics 表达。Schema 命中只能证明注册词汇及静态元数据存在，不能单独证明脚本可由真实服务器编译或安全运行。
