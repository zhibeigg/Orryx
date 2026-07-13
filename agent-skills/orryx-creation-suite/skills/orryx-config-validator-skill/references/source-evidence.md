# Source evidence

- `src/main/kotlin/org/gitee/orryx/core/skill/SkillLoaderManager.kt`：以 `configuration.name` 构造五类技能；reload 先 `skillMap.clear()` 再逐文件加载。
- `src/main/kotlin/org/gitee/orryx/core/skill/skills/AbstractSkillLoader.kt`：`Name` 回退 key；`Sort` 默认 0；变量键转大写。
- `src/main/kotlin/org/gitee/orryx/core/job/JobLoaderManager.kt`：职业 Map key 是 `configuration.name`，reload 先 clear。
- `src/main/kotlin/org/gitee/orryx/core/job/JobLoader.kt`：`Skills` 与 `Experience` 是字符串引用，Name 是显示字段。
- `src/main/kotlin/org/gitee/orryx/module/experience/ExperienceLoaderManager.kt`：经验 ID 是 `configuration.name`，reload 先 clear。
- `src/main/kotlin/org/gitee/orryx/module/experience/ExperienceLoader.kt`：Min 必须小于 Max，逐级执行 `ExperienceOfLevel`。
- `src/main/kotlin/org/gitee/orryx/module/state/StateManager.kt` 与 `module/state/Status.kt`：Status key 来自 `file.nameWithoutExtension`；reload 先清空 statusMap。
- `src/main/kotlin/org/gitee/orryx/core/station/stations/StationLoaderManager.kt` 与 `StationLoader.kt`：Station key 来自 basename；监听器和 Map 在加载前清空。
- `src/main/kotlin/org/gitee/orryx/core/message/PluginMessageHandler.kt`：两个 `requestAiming` 重载在非 legacy 版本返回 `UnsupportedVersionException`，异常文本明确“仅支持 1.12.2 版本”。

由上述 clear-then-load 顺序可推断 reload 没有跨文件事务回滚；这是静态审计风险结论，不宣称存在额外源码机制。
