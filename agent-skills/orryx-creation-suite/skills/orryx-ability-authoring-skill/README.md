# Orryx Ability Authoring Skill

生成五类 Orryx 技能 YAML，并校验 Description/Variables/Actions 一致性、Passive 的 Station 配套需求以及 Aim 的 1.12.2 限制。

```powershell
py -3 scripts/run_pipeline.py --input assets/request.example.json --output result.json
```

本组件依赖完整套件共享运行时，launcher 固定注入 `component: ability`。评估规范位于 `evals/orryx-ability-authoring-skill.eval.md`。
