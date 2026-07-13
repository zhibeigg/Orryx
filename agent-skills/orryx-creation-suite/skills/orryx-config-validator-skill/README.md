# Orryx Config Validator Skill

用于静态验证 Orryx 配置包的组件文档与 launcher。它检查 basename ID、职业技能/经验等跨引用、技能 Sort、Aim 版本限制和非事务 reload 风险。

## 快速运行

```powershell
py -3 scripts/run_pipeline.py --input assets/request.example.json --output result.json
```

运行时必须来自完整 `orryx-creation-suite` 的 `shared/orryx_toolkit`，或安装后同级 `orryx-creation-suite-runtime/orryx_toolkit`。本组件不单独携带 validator 实现。

评估规范位于 `evals/orryx-config-validator-skill.eval.md`。
