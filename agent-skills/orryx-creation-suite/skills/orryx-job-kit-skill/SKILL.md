---
name: orryx-job-kit-skill
description: >-
  Create and validate Orryx job-kit YAML using basename IDs, display Name/Icon separation, skill and experience references, attributes, mana/spirit/upgrade-point actions, and conservative second-job scaffolding. Use when assembling a job from existing abilities and curves without inventing unsupported ParentJob semantics.
license: MIT
activation:
  command: /orryx-job-kit-skill
  intents:
    - create Orryx job
    - assemble Orryx job kit
    - scaffold Orryx second job
metadata:
  author: NarraFork
  version: 1.1.0
  created: 2025-07-13
  last_reviewed: 2025-07-13
  review_interval_days: 90
provenance:
  repository: Orryx
  evidence: references/source-evidence.md
  contract: references/contract.md
---
# /orryx-job-kit-skill

组合职业显示信息、技能列表、经验曲线和资源公式，输出 Orryx 职业 artifact 与引用诊断。

## ID 与显示字段

- 文件 basename 是职业 ID，也是加载器 Map 的 key。
- `Options.Name` 是显示名，缺省时才回退到 key。
- `Options.Icon` 若由其他 UI/客户端约定使用，应作为显示资源名处理，不能代替职业 ID。
- `Options.Skills` 每项必须引用技能文件 basename。
- `Options.Experience` 必须引用经验文件 basename，默认是 `default`。

## 职业字段

`JobLoader` 读取：`Name`、`Skills`、`Attributes`、`MaxManaActions`、`RegainManaActions`、`UpgradePointActions`、`Experience`、`MaxSpiritActions`、`RegainSpiritActions`。真实 `剑修.yml` 展示了技能清单、等级属性、法力线性增长、固定精力与默认经验引用。

## 二转边界

当前加载器没有 `ParentJob` 字段或父子职业解析。用户要求二转时：

1. 生成独立职业文件 scaffold；
2. 输出 migration/requirement 说明，由外部命令、Station 或业务流程控制转职；
3. 可以记录来源职业为文档元数据，但不能写入并宣称 Orryx 支持 `ParentJob`；
4. 不自动继承技能、等级、经验或资源，除非请求明确给出外部实现契约。

## 工作流

1. 确定文件 ID、显示名、图标、技能顺序和经验 ID。
2. 校验技能/经验引用；project 模式缺失为 error。
3. 检查法力、精力、回复和升级点动作是否可按等级求值。
4. 生成职业 YAML；二转只生成保守 scaffold 与 requirements。
5. 保持引用顺序和诊断排序稳定。

## 运行

```powershell
py -3 scripts/run_pipeline.py --input assets/request.example.json --output result.json
```

## 禁止事项

- 不把显示名替换成内部 ID。
- 不虚构 `ParentJob`、自动继承或内建二转事务。
- 不忽略不存在的技能与经验引用。
- 不写入工作区或触发 reload。
