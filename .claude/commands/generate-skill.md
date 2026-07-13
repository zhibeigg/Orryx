# Orryx 配置创作入口

该命令是旧版 Orryx 技能生成器的兼容入口。新的配置创作能力由 `orryx-creation-suite` 提供。

## 路由规则

收到用户需求后按范围选择：

- 单个主动、指向、蓄力或被动技能：`/orryx-ability-authoring-skill`
- Kether 脚本：`/orryx-kether-authoring-skill`
- 职业与技能组：`/orryx-job-kit-skill`
- 经验、技能点、法力或精力成长：`/orryx-progression-curve-skill`
- Station、中转站、事件型被动、护盾或 Flag：`/orryx-station-mechanic-skill`
- 普攻、格挡、闪避、Status 或 Controller：`/orryx-combat-state-controller-skill`
- 完整职业或跨多个目录的套件：`/orryx-workflow-orchestrator-skill`
- 检查现有配置：`/orryx-config-validator-skill`

## 必须遵守

1. 先扫描现有配置 ID，再分配文件名。
2. 文件名 ID、`Options.Name`、`Icon` 和动画 ID 必须分别处理。
3. 被动技能存在事件响应时必须生成或声明 Station 依赖。
4. 当前源码中的 Aim 协议仅支持 Minecraft 1.12.2；其他版本不得生成可运行的 Aim 技能承诺。
5. 所有文件先生成到 staging，经过跨文件校验后才允许写入真实配置目录。
6. 不复制生产配置中的密码、Token 或 API Key。
7. 不自动执行 Orryx 全量重载。

最新 Kether 文档和 Schema：

- `https://zhibeigg.github.io/Orryx/kether/latest.md`
- `https://zhibeigg.github.io/Orryx/kether/actions-schema.json`
- `https://zhibeigg.github.io/Orryx/kether/manifest.json`
