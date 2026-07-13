# Source evidence

- `src/main/kotlin/org/gitee/orryx/core/skill/SkillLoaderManager.kt`：仅分派 Passive、DirectAim、Direct、PressingAim、Pressing 五类；key 来自 configuration.name。
- `src/main/kotlin/org/gitee/orryx/core/skill/skills/AbstractSkillLoader.kt`：加载 Name、Sort、Icon、Description、等级、升级动作和 Variables；变量键转大写。
- `src/main/kotlin/org/gitee/orryx/core/skill/skills/PassiveSkill.kt`：Passive 只有基础技能元数据与 type，没有 cast Actions 实现。
- `src/main/kotlin/org/gitee/orryx/core/skill/skills/IAim.kt`：Aim 定义 min/max/radius 动作。
- `src/main/kotlin/org/gitee/orryx/core/skill/skills/IPress.kt`：Press 定义打断 Trigger、Period、PressPeriodAction 与 MaxPressTickAction。
- `src/main/kotlin/org/gitee/orryx/core/skill/caster/DirectAimSkillCaster.kt` 与 `PressingAimSkillCaster.kt`：两类都调用 PluginMessageHandler 的客户端瞄准请求。
- `src/main/kotlin/org/gitee/orryx/core/message/PluginMessageHandler.kt`：非 legacy 版本返回“仅支持 1.12.2 版本”。
- `example/Orryx/skills/剑修/破空斩.yml`：Direct 案例；Description 展示 damage，Variables 定义 base/coef，Actions 分别应用物理倍率与固定真实伤害，并使用 sync。
- `example/Orryx/skills/拳修/蓄意轰拳.yml`：Pressing 案例；Period 16、MaxPressTick 80，32/48 tick 阈值对应三阶段倍率。
- `example/Orryx/skills/拳修/内劲.yml` 与 `example/Orryx/stations/拳修/拳修内劲.yml`：Passive 展示效果，Station 监听 Player Damage Pre 并实现职业相关伤害调整。
