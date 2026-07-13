# Orryx Kether Authoring Skill

面向 skill、station、state、job、experience 五种宿主上下文生成和审查 Kether Actions，重点处理变量可见性、Actions Schema 边界与主线程安全。

```powershell
py -3 scripts/run_pipeline.py --input assets/request.example.json --output result.json
```

launcher 固定注入 `component: kether`，并从完整套件的共享运行时执行。评估规范位于 `evals/orryx-kether-authoring-skill.eval.md`。
