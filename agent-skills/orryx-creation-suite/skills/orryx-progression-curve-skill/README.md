# Orryx Progression Curve Skill

设计经验、法力、精力和升级点公式，执行逐级模拟，并输出确定性的 line-chart JSON artifact。

```powershell
py -3 scripts/run_pipeline.py --input assets/request.example.json --output result.json
```

launcher 固定注入 `component: progression`，运行时来自完整套件。评估规范位于 `evals/orryx-progression-curve-skill.eval.md`。
