# Source evidence

- `src/main/kotlin/org/gitee/orryx/core/job/JobLoaderManager.kt`：以 `configuration.name` 作为职业 Map key，即文件 basename ID。
- `src/main/kotlin/org/gitee/orryx/core/job/JobLoader.kt`：Name 回退 key；Skills、Attributes、MaxManaActions、RegainManaActions、UpgradePointActions、Experience、MaxSpiritActions、RegainSpiritActions 均来自 Options。
- `example/Orryx/jobs/剑修.yml`：职业 ID 为文件名“剑修”，Name 同名；引用招架、破空斩等技能与 `default` 经验，并定义法力/精力/升级点动作。
- `src/main/kotlin/org/gitee/orryx/core/skill/SkillLoaderManager.kt`：技能 key 同样来自 `configuration.name`，因此 Job Skills 必须引用技能 basename。
- `src/main/kotlin/org/gitee/orryx/module/experience/ExperienceLoaderManager.kt`：经验 key 来自 `configuration.name`，因此 Job Experience 必须引用经验 basename。
- 对 `src/main/kotlin/org/gitee/orryx/core/job/` 的加载接口检查未发现 `ParentJob` 或父职业解析字段；二转只能作为外部流程 scaffold，不能写成已支持的原生语义。
