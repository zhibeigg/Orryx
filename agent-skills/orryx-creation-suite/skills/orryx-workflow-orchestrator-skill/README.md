# orryx-workflow-orchestrator-skill

Orryx 多组件创建编排文档包。先建立 ID/引用图，再严格按 validator→kether→ability/progression/job/station/combat/selector/ui 执行，error 清零后才允许 materialize。

## 运行

```text
py -3 scripts/run_pipeline.py --input assets/request.example.json --output orchestration-report.json
py -3 scripts/run_evals.py --validate
py -3 scripts/run_evals.py --rollout
```

启动器可从源码树或安装后的同级 `orryx-creation-suite-runtime` 加载共享 runtime。

## 安全边界

- 默认只计划，不写入。
- 仍有 error 时拒绝 materialize。
- 永不执行服务器重载、重启或生产连接。
- 单独提取不能安装，必须使用套件根安装器。

`evals/` 包含完整跨组件计划、部分组件计划和“有悬空引用仍请求 materialize”的负例。
