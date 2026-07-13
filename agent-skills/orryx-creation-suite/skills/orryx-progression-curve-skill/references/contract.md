# Progression component contract

## Request

launcher 固定注入 `component: progression`。`request` 包含曲线 ID、Min/Max、`ExperienceOfLevel` 规则，以及可选的 mana/spirit/upgradePoint 表达式和模拟目标。公式使用受限 Orryx/Kether 表达式描述，不执行任意主机代码。

## Simulation

逐级记录 `level`、本级经验、累计经验、max/regain mana、max/regain spirit、本级/累计升级点。Min 与 Max 都进入表；累计经验遵循 Orryx 从 Min 到目标级前一级求和的语义。

## Artifacts

- `experiences/<id>.yml`
- 可选职业资源 YAML 片段
- `reports/<id>-simulation.json`
- `reports/<id>-line-chart.json`，mediaType 固定 `application/vnd.orryx.line-chart+json`

折线图必须包含 `kind: line-chart`、x 轴、等长 series 和 units。输出无当前时间、随机值和绝对路径。
