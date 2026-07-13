# Orryx Job Kit Skill

组合 Orryx 职业 ID、显示信息、技能、经验曲线、属性和资源动作，并为二转请求提供不虚构 ParentJob 的保守 scaffold。

```powershell
py -3 scripts/run_pipeline.py --input assets/request.example.json --output result.json
```

launcher 固定注入 `component: job`，运行时来自完整套件。评估规范位于 `evals/orryx-job-kit-skill.eval.md`。
