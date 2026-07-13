# Source evidence

- `src/main/kotlin/org/gitee/orryx/core/kether/parameter/SkillParameter.kt`：技能上下文携带 skill、player、level、trigger、origin。
- `src/main/kotlin/org/gitee/orryx/core/kether/parameter/StationParameter.kt`：Station 上下文携带 stationLoader、sender、event。
- `src/main/kotlin/org/gitee/orryx/core/kether/parameter/StateParameter.kt`：状态上下文来自 PlayerData，语义不同于技能和 Station。
- `src/main/kotlin/org/gitee/orryx/core/kether/actions/ParameterOperators.kt`：`SKILL`/`LEVEL` 强转 SkillParameter，`STATION` 强转 StationParameter，证明参数访问依赖上下文。
- `src/main/kotlin/org/gitee/orryx/core/kether/actions/station/EventActions.kt`：事件取消动作检查参数是否为 StationParameter，且事件是否 Cancellable。
- `src/main/kotlin/org/gitee/orryx/module/experience/ExperienceLoader.kt`：经验表达式用 sender 与 `level` 变量单独执行。
- `src/main/kotlin/org/gitee/orryx/core/job/JobLoader.kt`：职业法力、精力、升级点字段保存为独立动作表达式。
- `src/main/kotlin/org/gitee/orryx/core/station/stations/StationLoaderManager.kt`：Station 可配置 Async；异步时通过 pluginScope 启动，因此脚本中的 Bukkit 操作必须审慎切回主线程。
- `src/main/kotlin/org/gitee/orryx/core/kether/actions/StateActions.kt`：状态动作中使用 `ensureSync` 访问 Bukkit/状态数据，提供主线程处理证据。
- `src/main/kotlin/org/gitee/orryx/module/wiki/ActionsSchemaGenerator.kt`：Schema 顶层只生成 types、categories、actions、selectors、triggers、properties 及元数据。
- `docs/Kether-Docs-Publishing.md`：Schema v3 描述稳定 ID、syntax、inputs、examples、execution 与 requirements；未定义完整 Orryx YAML 文档结构。
