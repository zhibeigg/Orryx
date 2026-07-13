# Agent instructions

- 先读 `SKILL.md`、`references/contract.md` 与 `references/source-evidence.md`。
- 只通过 `scripts/run_pipeline.py` 调用共享运行时；不得复制 validator 实现到本组件。
- 始终保留 basename ID、显示名和相对路径的区别。
- project 模式严格检查跨引用；standalone 模式只对未提供的外部引用降级。
- 明示 Aim 仅支持 1.12.2，以及 reload 先清空再加载的非事务风险。
- 不写配置、不触发 reload、不联网、不输出主机绝对路径。
- 无效输入必须产生结构化 diagnostics；不要用异常代替业务诊断。
