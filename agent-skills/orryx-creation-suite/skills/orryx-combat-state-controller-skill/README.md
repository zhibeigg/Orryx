# orryx-combat-state-controller-skill

Orryx status + controller 联合设计文档包，覆盖普攻、格挡、闪避、`running`、时间窗和动画资产。

## 运行

```text
py -3 scripts/run_pipeline.py --input assets/request.example.json --output combat-report.json
py -3 scripts/run_evals.py --validate
py -3 scripts/run_evals.py --rollout
```

启动器支持套件源码树 `shared/orryx_toolkit`，也支持安装后技能同级 `orryx-creation-suite-runtime`。

## 安装

执行本目录 `install.sh`/`install.ps1` 会调用套件根安装器。单独提取组件会因缺少共享 runtime 而明确失败。

## 核心产物

- status/controller artifacts
- state 与 animation 引用图
- Connection/Check/Invincible 时间轴
- 客户端动画、模型、声音和粒子依赖
- 3 个 golden rollout 案例（含非法时间窗负例）
