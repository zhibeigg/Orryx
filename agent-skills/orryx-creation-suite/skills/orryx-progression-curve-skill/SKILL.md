---
name: orryx-progression-curve-skill
description: >-
  Design and simulate Orryx experience, mana, spirit, and upgrade-point progression. Use for ExperienceOfLevel curves, job resource formulas, level-by-level balance tables, monotonicity and boundary diagnostics, and deterministic line-chart artifacts based on experiences/default.yml and jobs/剑修.yml.
license: MIT
activation:
  command: /orryx-progression-curve-skill
  intents:
    - design Orryx experience curve
    - simulate Orryx job progression
    - chart mana spirit or upgrade points
metadata:
  author: NarraFork
  version: 1.0.0
  created: 2025-07-13
  last_reviewed: 2025-07-13
  review_interval_days: 90
provenance:
  repository: Orryx
  evidence: references/source-evidence.md
  contract: references/contract.md
---
# /orryx-progression-curve-skill

生成经验曲线、职业资源公式、逐级模拟表和可复现折线图协议 artifact。

## Orryx 字段

经验文件：

- `Options.Min`
- `Options.Max`，必须大于 `Min`
- `Options.ExperienceOfLevel`，在 `level` 上下文中计算当前级所需经验

职业资源：

- `MaxManaActions` / `RegainManaActions`
- `MaxSpiritActions` / `RegainSpiritActions`
- `UpgradePointActions`
- `Experience`，引用经验文件 basename ID

真实默认经验曲线是 `calc "200*level"`，范围 `0..20`。`剑修` 示例使用 `MaxManaActions: calc "100+10*level"`、固定精力上限 `100`、回复 `5`、每级升级点 `1`，并引用 `default`。

## 逐级模拟

对 `Min..Max` 每一级计算：

- 本级经验需求；
- 到达该级前的累计经验；
- 法力上限与回复；
- 精力上限与回复；
- 本级与累计升级点。

报告负值、非整数经验、断崖式跳变、意外下降、零值区间和溢出风险。边界级必须纳入模拟。

## line-chart artifact protocol

折线图不是二进制图片，而是确定性 JSON artifact：

```json
{
  "kind": "line-chart",
  "title": "Progression by level",
  "x": {"name": "level", "values": [0, 1, 2]},
  "series": [{"name": "experience", "values": [0, 200, 400]}],
  "units": {"experience": "xp"}
}
```

要求：level 升序、每个 series 与 x 等长、数值有限、名称稳定、无生成时间。artifact 的 `mediaType` 为 `application/vnd.orryx.line-chart+json`。

## 工作流

1. 读取范围、公式、资源规则和目标节奏。
2. 逐级求值并累计，不只抽样首尾等级。
3. 生成经验 YAML、可选职业片段、模拟表与折线图 artifacts。
4. 将经验引用与异常曲线写入 diagnostics/checks。
5. 不调用游戏服务器或执行 reload。

## 运行

```powershell
py -3 scripts/run_pipeline.py --input assets/request.example.json --output result.json
```
