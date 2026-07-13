# 源码与生产案例依据

## ID 与加载器

- `core/skill/SkillLoaderManager.kt`：技能类型由 `Options.Type` 分派，技能 ID 使用配置文件名。
- `core/job/JobLoaderManager.kt` 与 `JobLoader.kt`：职业 ID 使用文件名；`Skills` 和 `Experience` 在加载时缺少集中式引用校验。
- `module/experience/ExperienceLoader.kt`：只硬校验 `Min < Max`，公式通过 Kether 注入 `level`。
- `core/station/stations/StationLoader.kt`：Event 必填并转大写；Priority 使用枚举；Actions 必填；Async 默认 false。
- `module/state/Status.kt`：Condition 必填；DragonCore 启用时 Controller 必填；States 与 Action 在运行时解析。

## 五种技能

- `PASSIVE`：不可主动释放，不需要 Actions。
- `DIRECT`：立即消费并执行。
- `DIRECT AIM`：读取 AimSizeAction 和 AimRadiusAction。
- `PRESSING`：读取 Period、PressPeriodAction、MaxPressTickAction、PressBrockTriggers。
- `PRESSING AIM`：同时加载 Aim 与 Press 字段，但当前 Caster 并未消费全部 Press 周期字段。

`core/message/PluginMessageHandler.kt` 当前只在 `MinecraftVersion.versionId == 11202` 时接受 Aim 请求，其他版本返回 UnsupportedVersionException。

## Kether Action Schema

- `scripts/build_action_schema.py` 直接扫描 `core/kether/actions/**/*.kt` 中的 `@KetherParser` 注解，不维护手写 Action 白名单。
- 生成结果记录主名称、别名、namespace、shared、parser factory、源码路径/行号，并依据同一源码文件中的 Bukkit、危险变更与 Selector 证据标注执行前置条件。
- `sourceSha256` 覆盖 Action 与 parameter 源文件；`validate_suite.py` 和 `build_action_schema.py --check` 会阻断陈旧 Schema。
- 静态证据不能替代真实 Kether 编译与服务器插件探测，兼容模块仍需作为前置条件确认。

## 生产案例

### 职业链路

`example/Orryx/jobs/剑修.yml`：

- 引用七个技能文件 ID。
- 使用 `default` 经验算法。
- 用 Kether 定义最大法力、恢复、精力和技能点。

### 主动技能

`example/Orryx/skills/剑修/破空斩.yml`：

- DIRECT。
- 描述、冷却、固定段和属性倍率随等级成长。
- 使用 Selector、DamageProcessor、动画、声音和客户端效果。

### 蓄力技能

`example/Orryx/skills/拳修/蓄意轰拳.yml`：

- PRESSING。
- Period、PressPeriodAction 和 MaxPressTickAction。
- 使用 pressTick 选择三阶段倍率。

### 被动机制

- `skills/拳修/内劲.yml` 只提供 PASSIVE 元数据。
- `stations/拳修/拳修内劲.yml` 监听 Player Damage Pre 实现真实效果。

因此生成被动时必须检查 Station 或 Status 需求。

### 战斗状态机

- `status/剑修.yml` 定义普攻三连、招架、闪避和翻滚。
- `controllers/长剑.yml` 定义相同动画 ID 的控制器层和客户端 Trigger。

二者必须联合生成和校验。

### 经验曲线

`experiences/default.yml` 使用分段 Kether 公式计算 0-50 级需求经验。生成器必须逐级求值并检查非正值、跳变和累计溢出。

## 已知反例

- 递归目录中相同 basename 会发生静默覆盖。
- 二转职业示例的 `Skills` 为空，没有原生 ParentJob 字段。
- 部分示例存在技能 Sort 重复。
- `stations/法力系统/法力regain.yml` 使用小写 `async`，Loader 实际读取 `Async`。
- `skills/test.yml` 引用未在 buffs.yml 定义的 Buff，不能作为正向模板。
