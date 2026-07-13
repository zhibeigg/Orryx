# Agent instructions

- 职业 ID 始终取文件 basename；Name/Icon 是显示字段。
- Skills 与 Experience 必须按 basename 检查引用。
- 保留技能列表顺序，它是职业 kit 的明确设计输入。
- 数值动作分别处理法力、精力、回复与升级点。
- 二转只生成独立职业 scaffold 和外部迁移 requirement。
- 不生成或宣称 `ParentJob`、自动继承、自动迁移是 Orryx 原生能力。
- 以 `JobLoader.kt` 与 `example/Orryx/jobs/剑修.yml` 为事实依据。
- 只输出 artifacts/diagnostics，不写入、不 reload。
