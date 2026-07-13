# orryx-station-mechanic-skill

Orryx 中转站机制文档包，用于事件方向、Pre/Post、Priority/Weight、Async、护盾、Flag 与累计伤害设计。

## 运行

```text
py -3 scripts/run_pipeline.py --input assets/request.example.json --output station-report.json
py -3 scripts/run_evals.py --validate
py -3 scripts/run_evals.py --rollout
```

启动器会优先加载套件源码树的 `shared/orryx_toolkit`；安装后则加载技能同级的 `orryx-creation-suite-runtime`。输入与输出均为 UTF-8 JSON。

## 安装

从完整套件中运行 `install.sh` 或 `install.ps1`。组件安装脚本只转发到套件根安装器；单独提取时会明确报错，因为缺少共享 runtime。

## 内容

- `SKILL.md` / `AGENTS.md`：激活与代理规则
- `references/contract.md`：组件输入、输出和错误门
- `references/source-evidence.md`：Orryx 源码与案例依据
- `assets/request.example.json`：可执行示例
- `evals/`：3 个可 rollout golden（含负例）
