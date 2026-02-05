# Orryx API 开发文档

> 本文档面向 Kotlin/Java 开发者，帮助您使用 Orryx API 进行二次开发。

---

## 目录

1. [概述](#1-概述)
2. [快速入门](#2-快速入门)
3. [技能系统](#3-技能系统)
4. [职业系统](#4-职业系统)
5. [事件系统](#5-事件系统)
6. [其他API模块](#6-其他api模块)
7. [脚本引擎](#7-脚本引擎)
8. [扩展开发](#8-扩展开发)
9. [附录](#9-附录)

---

## 1. 概述

### 1.1 Orryx 简介

Orryx 是一个跨时代的 Minecraft 技能插件，基于 Kotlin 2.1.20 和 TabooLib 6.2 框架开发，支持 Minecraft 1.12-1.21 版本。

**核心特性：**
- 5 种技能类型（被动、直接、指向、蓄力、蓄力指向）
- 完整的职业系统
- 80+ 触发器
- Kether + Kotlin 双脚本引擎
- 8 种碰撞箱类型
- 30+ 事件 API

### 1.2 架构设计

```
org.gitee.orryx/
├── api/                    # 公开 API（对外暴露）
│   ├── adapters/          # 实体和向量适配器
│   ├── collider/          # 碰撞系统（8 种碰撞箱）
│   ├── events/            # 事件系统（30+ 事件）
│   └── interfaces/        # API 接口定义（10 个）
├── core/                   # 核心模块（内部实现）
│   ├── skill/             # 技能系统
│   ├── job/               # 职业系统
│   ├── profile/           # 玩家档案系统
│   ├── station/           # 触发器系统
│   ├── kether/            # Kether 脚本引擎
│   └── ...
├── module/                 # 功能模块
│   ├── mana/              # 法力值管理
│   ├── spirit/            # 精力值管理
│   ├── state/             # 状态机系统
│   └── ui/                # UI 渲染
└── compat/                 # 第三方插件兼容
```

### 1.3 核心概念

| 概念 | 说明 |
|------|------|
| **IOrryxAPI** | 主 API 入口，提供所有子 API 的访问 |
| **ISkill** | 技能配置（元数据），定义技能的基本属性 |
| **IPlayerSkill** | 玩家技能实例，包含玩家特定的技能数据 |
| **IJob** | 职业配置，定义职业的基本属性 |
| **IPlayerJob** | 玩家职业实例，包含玩家特定的职业数据 |
| **IPlayerProfile** | 玩家档案，管理技能点、Flag 等持久化数据 |
| **CompletableFuture** | 异步操作返回类型，大部分 API 方法都是异步的 |

---

## 2. 快速入门

### 2.1 Maven/Gradle 依赖配置

**Gradle (Kotlin DSL):**
```kotlin
repositories {
    maven("https://maven.mcwar.cn/releases")
}

dependencies {
    compileOnly("org.gitee.orryx:orryx:1.34.88:api")
}
```

**Gradle (Groovy):**
```groovy
repositories {
    maven { url 'https://maven.mcwar.cn/releases' }
}

dependencies {
    compileOnly 'org.gitee.orryx:orryx:1.34.88:api'
}
```

### 2.2 获取 API 实例

```kotlin
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.interfaces.IOrryxAPI

// 获取主 API 实例
val api: IOrryxAPI = Orryx.api()

// 访问各个子 API
val skillAPI = api.skillAPI       // 技能 API
val jobAPI = api.jobAPI           // 职业 API
val profileAPI = api.profileAPI   // 玩家档案 API
val keyAPI = api.keyAPI           // 按键 API
val taskAPI = api.taskAPI         // 任务 API
val timerAPI = api.timerAPI       // 计时器 API
val consumptionValueAPI = api.consumptionValueAPI  // 法力/精力 API
val miscAPI = api.miscAPI         // 杂项 API
val reloadAPI = api.reloadAPI     // 重载 API
```

### 2.3 第一个示例：监听技能释放

```kotlin
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCastEvents
import taboolib.common.platform.event.SubscribeEvent

object MyListener {

    @SubscribeEvent
    fun onSkillCast(event: OrryxPlayerSkillCastEvents.Cast) {
        val player = event.player
        val skill = event.skill

        player.sendMessage("你释放了技能: ${skill.skill.name}")
        player.sendMessage("技能等级: ${skill.level}")
    }
}
```

### 2.4 开发环境搭建建议

1. **IDE**: 推荐使用 IntelliJ IDEA
2. **Kotlin 版本**: 2.1.20 或更高
3. **Java 版本**: Java 8 或更高
4. **测试服务器**: 建议使用 Paper 1.20.4+

---

## 3. 技能系统

技能系统是 Orryx 的核心功能，提供完整的技能管理能力。

### 3.1 ISkillAPI 接口详解

```kotlin
interface ISkillAPI {
    /**
     * 获取玩家职业技能并进行操作
     * 注意：此方法不会自动保存更改，如需持久化请手动调用 save()
     */
    fun <T> modifySkill(
        player: Player,
        skill: String,
        job: IPlayerJob? = null,
        function: Function<IPlayerSkill, T>
    ): CompletableFuture<T?>

    /**
     * 直接释放技能（不进行任何条件检查）
     */
    fun castSkill(player: Player, skill: String, level: Int)

    /**
     * 尝试释放技能（进行完整的条件检查）
     */
    fun tryCastSkill(player: Player, skill: String): CompletableFuture<CastResult?>

    /**
     * 获取技能配置
     */
    fun getSkill(skill: String): ISkill?
}
```

### 3.2 技能类型

| 类型 | 说明 | 使用场景 |
|------|------|----------|
| **Passive** | 被动技能 | 自动触发，无需手动释放 |
| **Direct** | 直接释放 | 按键即释放 |
| **Direct Aim** | 直接指向性 | 带指示器的指向性技能 |
| **Pressing** | 蓄力释放 | 长按蓄力后释放 |
| **Pressing Aim** | 蓄力指向性 | 蓄力型指向性技能 |

### 3.3 技能生命周期

```
玩家按键
    ↓
Check 事件 (OrryxPlayerSkillCastEvents.Check)
    ↓ (可取消)
条件检查（冷却、法力值、沉默状态等）
    ↓
Cast 事件 (OrryxPlayerSkillCastEvents.Cast)
    ↓
Kether 脚本执行
    ↓
技能效果生效
```

### 3.4 技能释放与冷却管理

**释放技能：**
```kotlin
val api = Orryx.api()

// 方式1：直接释放（跳过所有检查）
api.skillAPI.castSkill(player, "fireball", 5)

// 方式2：尝试释放（进行完整检查）
api.skillAPI.tryCastSkill(player, "fireball").thenAccept { result ->
    when (result) {
        CastResult.SUCCESS -> player.sendMessage("技能释放成功")
        CastResult.COOLING -> player.sendMessage("技能冷却中")
        CastResult.NO_MANA -> player.sendMessage("法力值不足")
        CastResult.SILENCE -> player.sendMessage("你被沉默了")
        else -> player.sendMessage("释放失败: $result")
    }
}
```

**冷却管理：**
```kotlin
// 通过 modifySkill 操作技能冷却
api.skillAPI.modifySkill(player, "fireball") { skill ->
    // 获取冷却计时器
    val timer = api.timerAPI.getSkillTimer(player, skill.key)

    // 检查是否在冷却中
    if (timer?.hasCountdown() == true) {
        player.sendMessage("剩余冷却: ${timer.countdown}ms")
    }

    // 重置冷却
    timer?.reset()

    // 增加冷却时间
    timer?.add(1000) // 增加 1 秒

    // 减少冷却时间
    timer?.take(500) // 减少 0.5 秒
}
```

### 3.5 技能等级与升级

```kotlin
api.skillAPI.modifySkill(player, "fireball") { skill ->
    // 获取当前等级
    val currentLevel = skill.level
    player.sendMessage("当前等级: $currentLevel")

    // 升级技能
    skill.upLevel(1).thenAccept { result ->
        when (result) {
            SkillLevelResult.SUCCESS -> player.sendMessage("升级成功")
            SkillLevelResult.MAX_LEVEL -> player.sendMessage("已达最高等级")
            SkillLevelResult.NOT_ENOUGH_POINT -> player.sendMessage("技能点不足")
            else -> player.sendMessage("升级失败: $result")
        }
    }

    // 设置技能等级
    skill.setLevel(10).thenAccept { result ->
        // 处理结果
    }

    // 降级技能
    skill.downLevel(1).thenAccept { result ->
        // 处理结果
    }

    // 手动保存更改
    skill.save()
}
```

### 3.6 实战示例：自定义技能逻辑

**示例1：技能释放前添加额外检查**
```kotlin
@SubscribeEvent
fun onSkillCheck(event: OrryxPlayerSkillCastEvents.Check) {
    val player = event.player
    val skill = event.skill

    // 检查玩家是否在特定世界
    if (player.world.name == "lobby") {
        event.isCancelled = true
        player.sendMessage("大厅中无法使用技能")
        return
    }

    // 检查特定技能的额外条件
    if (skill.key == "ultimate_skill" && player.health < 10) {
        event.isCancelled = true
        player.sendMessage("生命值过低，无法释放终极技能")
    }
}
```

**示例2：技能释放后添加额外效果**
```kotlin
@SubscribeEvent
fun onSkillCast(event: OrryxPlayerSkillCastEvents.Cast) {
    val player = event.player
    val skill = event.skill

    // 为特定技能添加额外效果
    if (skill.key == "heal") {
        // 播放额外粒子效果
        player.world.spawnParticle(
            Particle.HEART,
            player.location.add(0.0, 2.0, 0.0),
            10
        )
    }

    // 记录技能使用日志
    logger.info("${player.name} 使用了技能 ${skill.skill.name}")
}
```

---

## 4. 职业系统

职业系统管理玩家的职业数据，包括等级、经验、技能绑定等。

### 4.1 IJobAPI 接口详解

```kotlin
interface IJobAPI {
    /**
     * 获取玩家职业并进行操作
     * 注意：此方法不会自动保存更改，如需持久化请手动调用 save()
     */
    fun <T> modifyJob(
        player: Player,
        job: String? = null,  // 为 null 时使用玩家当前职业
        function: Function<IPlayerJob, T>
    ): CompletableFuture<T?>

    /**
     * 获取职业配置
     */
    fun getJob(job: String): IJob?
}
```

### 4.2 职业数据结构

**IJob（职业配置）：**
```kotlin
interface IJob {
    val key: String              // 职业键名
    val name: String             // 职业显示名
    val skills: List<String>     // 职业技能列表
    val upgradePointActions: String  // 升级获取技能点公式
    val maxManaActions: String   // 最大法力值公式
    val regainManaActions: String // 法力恢复公式
    val attributes: List<String> // 职业属性列表
    val experience: String       // 经验算法
    val maxSpiritActions: String // 最大精力值公式
    val regainSpiritActions: String // 精力恢复公式
}
```

**IPlayerJob（玩家职业实例）：**
```kotlin
interface IPlayerJob {
    val uuid: UUID               // 玩家 UUID
    val player: Player           // 玩家实体
    val key: String              // 职业键名
    val job: IJob                // 职业配置
    val group: String            // 当前技能组
    val level: Int               // 当前等级
    val maxLevel: Int            // 最大等级
    val experience: Int          // 累计经验值
    val experienceOfLevel: Int   // 当前等级经验值
    val maxExperienceOfLevel: Int // 当前等级最大经验值
    val bindKeyOfGroup: Map<IGroup, Map<IBindKey, String?>> // 按键绑定
}
```

### 4.3 职业切换与管理

```kotlin
val api = Orryx.api()

// 获取职业配置
val warriorJob = api.jobAPI.getJob("warrior")
if (warriorJob != null) {
    player.sendMessage("职业名称: ${warriorJob.name}")
    player.sendMessage("职业技能: ${warriorJob.skills.joinToString()}")
}

// 操作玩家职业数据
api.jobAPI.modifyJob(player) { playerJob ->
    player.sendMessage("当前职业: ${playerJob.job.name}")
    player.sendMessage("职业等级: ${playerJob.level}")
    player.sendMessage("当前经验: ${playerJob.experienceOfLevel}/${playerJob.maxExperienceOfLevel}")

    // 获取职业属性
    val attributes = playerJob.getAttributes()
    player.sendMessage("职业属性: $attributes")

    // 获取最大法力值
    val maxMana = playerJob.getMaxMana()
    player.sendMessage("最大法力值: $maxMana")
}
```

### 4.4 经验与等级系统

```kotlin
api.jobAPI.modifyJob(player) { playerJob ->
    // 给予经验
    playerJob.giveExperience(100).thenAccept { result ->
        when (result) {
            ExperienceResult.SUCCESS -> player.sendMessage("获得 100 经验")
            ExperienceResult.LEVEL_UP -> player.sendMessage("升级了！")
            else -> player.sendMessage("经验获取失败")
        }
    }

    // 扣除经验
    playerJob.takeExperience(50).thenAccept { result ->
        // 处理结果
    }

    // 设置经验
    playerJob.setExperience(1000).thenAccept { result ->
        // 处理结果
    }

    // 给予等级
    playerJob.giveLevel(1).thenAccept { result ->
        when (result) {
            LevelResult.SUCCESS -> player.sendMessage("等级提升")
            LevelResult.MAX_LEVEL -> player.sendMessage("已达最高等级")
            else -> player.sendMessage("升级失败")
        }
    }

    // 设置等级
    playerJob.setLevel(10).thenAccept { result ->
        // 处理结果
    }

    // 获取升级会带来的技能点
    val points = playerJob.getUpgradePoint(1, 10)
    player.sendMessage("1-10级共获得 $points 技能点")

    // 保存更改
    playerJob.save()
}
```

### 4.5 实战示例：职业相关功能

**示例1：职业切换监听**
```kotlin
@SubscribeEvent
fun onJobChange(event: OrryxPlayerJobChangeEvents.Post) {
    val player = event.player
    val oldJob = event.oldJob
    val newJob = event.newJob

    player.sendMessage("职业已从 ${oldJob?.name ?: "无"} 切换为 ${newJob.name}")

    // 切换职业后重置某些状态
    Orryx.api().profileAPI.cancelSilence(player)
}
```

**示例2：职业升级奖励**
```kotlin
@SubscribeEvent
fun onJobLevelUp(event: OrryxPlayerJobLevelEvents.Post) {
    val player = event.player
    val oldLevel = event.oldLevel
    val newLevel = event.newLevel

    if (newLevel > oldLevel) {
        // 升级奖励
        player.sendMessage("恭喜升级到 $newLevel 级！")

        // 每10级给予额外奖励
        if (newLevel % 10 == 0) {
            player.sendMessage("达成里程碑！获得额外奖励！")
            // 添加奖励逻辑
        }
    }
}
```

---

## 5. 事件系统

Orryx 提供了丰富的事件系统，允许开发者监听和处理各种游戏事件。

### 5.1 事件概览

| 分类 | 事件数量 | 说明 |
|------|----------|------|
| 技能事件 | 9 个 | 技能释放、冷却、等级、绑定等 |
| 职业事件 | 5 个 | 职业更改、清除、经验、等级、保存 |
| 玩家状态事件 | 10+ 个 | 法力值、精力值、技能点、Flag 等 |
| 伤害事件 | 2 个 | 伤害前、伤害后 |
| 按键事件 | 4 个 | 保存、按下、抬起、持续 |
| 全局事件 | 4 个 | 重载、脚本终止、触发器注册等 |

### 5.2 技能事件

**OrryxPlayerSkillCastEvents - 技能释放事件**
```kotlin
// 技能释放检查（可取消）
@SubscribeEvent
fun onSkillCheck(event: OrryxPlayerSkillCastEvents.Check) {
    val player = event.player
    val skill = event.skill
    val parameter = event.skillParameter

    // 取消技能释放
    if (someCondition) {
        event.isCancelled = true
    }
}

// 技能释放后
@SubscribeEvent
fun onSkillCast(event: OrryxPlayerSkillCastEvents.Cast) {
    val player = event.player
    val skill = event.skill
    // 处理技能释放后的逻辑
}
```

**OrryxPlayerSkillCooldownEvents - 技能冷却事件**
```kotlin
// 冷却增加
@SubscribeEvent
fun onCooldownAdd(event: OrryxPlayerSkillCooldownEvents.Add) {
    player.sendMessage("技能 ${event.skill} 冷却增加了 ${event.amount}ms")
}

// 冷却减少
@SubscribeEvent
fun onCooldownTake(event: OrryxPlayerSkillCooldownEvents.Take) {
    // 处理冷却减少
}

// 冷却设置
@SubscribeEvent
fun onCooldownSet(event: OrryxPlayerSkillCooldownEvents.Set) {
    // 处理冷却设置
}
```

**OrryxPlayerSkillLevelEvents - 技能等级事件**
```kotlin
// 技能等级变化前
@SubscribeEvent
fun onSkillLevelPre(event: OrryxPlayerSkillLevelEvents.Pre) {
    val oldLevel = event.oldLevel
    val newLevel = event.newLevel
    // 可以取消等级变化
    if (someCondition) {
        event.isCancelled = true
    }
}

// 技能等级变化后
@SubscribeEvent
fun onSkillLevelPost(event: OrryxPlayerSkillLevelEvents.Post) {
    // 处理等级变化后的逻辑
}
```

**OrryxPlayerSkillBindKeyEvent - 技能绑定按键事件**
```kotlin
@SubscribeEvent
fun onSkillBindKey(event: OrryxPlayerSkillBindKeyEvent) {
    val player = event.player
    val skill = event.skill
    val key = event.key
    player.sendMessage("技能 ${skill.skill.name} 绑定到按键 $key")
}
```

### 5.3 职业事件

**OrryxPlayerJobChangeEvents - 职业更改事件**
```kotlin
// 职业更改前（可取消）
@SubscribeEvent
fun onJobChangePre(event: OrryxPlayerJobChangeEvents.Pre) {
    val player = event.player
    val oldJob = event.oldJob
    val newJob = event.newJob

    // 检查是否允许切换
    if (!canChangeJob(player)) {
        event.isCancelled = true
        player.sendMessage("当前无法切换职业")
    }
}

// 职业更改后
@SubscribeEvent
fun onJobChangePost(event: OrryxPlayerJobChangeEvents.Post) {
    val player = event.player
    val newJob = event.newJob
    player.sendMessage("成功切换到职业: ${newJob.name}")
}
```

**OrryxPlayerJobExperienceEvents - 职业经验事件**
```kotlin
// 经验变化前
@SubscribeEvent
fun onExpPre(event: OrryxPlayerJobExperienceEvents.Pre) {
    // 可以修改经验变化量
    event.amount = (event.amount * 1.5).toInt() // 1.5倍经验
}

// 经验变化后
@SubscribeEvent
fun onExpPost(event: OrryxPlayerJobExperienceEvents.Post) {
    player.sendMessage("经验变化: ${event.oldExp} -> ${event.newExp}")
}
```

**OrryxPlayerJobLevelEvents - 职业等级事件**
```kotlin
// 等级变化前
@SubscribeEvent
fun onLevelPre(event: OrryxPlayerJobLevelEvents.Pre) {
    // 可取消等级变化
}

// 等级变化后
@SubscribeEvent
fun onLevelPost(event: OrryxPlayerJobLevelEvents.Post) {
    if (event.newLevel > event.oldLevel) {
        // 升级逻辑
    } else {
        // 降级逻辑
    }
}
```

### 5.4 玩家状态事件

**OrryxPlayerManaEvents - 法力值事件**
```kotlin
// 法力值变化前
@SubscribeEvent
fun onManaPre(event: OrryxPlayerManaEvents.Pre) {
    // 可以修改法力值变化量或取消
}

// 法力值变化后
@SubscribeEvent
fun onManaPost(event: OrryxPlayerManaEvents.Post) {
    val player = event.player
    val oldMana = event.oldMana
    val newMana = event.newMana
    // 处理法力值变化
}
```

**OrryxPlayerSpiritEvents - 精力值事件**
```kotlin
@SubscribeEvent
fun onSpiritChange(event: OrryxPlayerSpiritEvents.Post) {
    // 处理精力值变化
}
```

**OrryxPlayerPointEvents - 技能点事件**
```kotlin
@SubscribeEvent
fun onPointChange(event: OrryxPlayerPointEvents.Post) {
    val player = event.player
    val oldPoint = event.oldPoint
    val newPoint = event.newPoint
    player.sendMessage("技能点: $oldPoint -> $newPoint")
}
```

**OrryxPlayerFlagChangeEvents - 玩家 Flag 变更事件**
```kotlin
@SubscribeEvent
fun onFlagChange(event: OrryxPlayerFlagChangeEvents) {
    val player = event.player
    val key = event.key
    val oldValue = event.oldValue
    val newValue = event.newValue
    // 处理 Flag 变更
}
```

### 5.5 伤害事件

**OrryxDamageEvents - 伤害事件**
```kotlin
// 伤害前（可取消、可修改）
@SubscribeEvent
fun onDamagePre(event: OrryxDamageEvents.Pre) {
    val attacker = event.attacker
    val victim = event.victim
    val damage = event.damage
    val damageType = event.damageType

    // 修改伤害
    event.damage = damage * 1.2

    // 根据伤害类型处理
    when (damageType) {
        DamageType.PHYSICAL -> { /* 物理伤害 */ }
        DamageType.MAGICAL -> { /* 魔法伤害 */ }
        DamageType.FIRE -> { /* 火焰伤害 */ }
        DamageType.TRUE -> { /* 真实伤害 */ }
        else -> { /* 其他类型 */ }
    }

    // 取消伤害
    if (shouldCancel) {
        event.isCancelled = true
    }
}

// 伤害后
@SubscribeEvent
fun onDamagePost(event: OrryxDamageEvents.Post) {
    // 伤害已造成，处理后续逻辑
}
```

### 5.6 实战示例：事件监听与处理

**示例1：技能连击系统**
```kotlin
object ComboSystem {
    private val comboMap = mutableMapOf<UUID, Int>()
    private val lastCastTime = mutableMapOf<UUID, Long>()
    private const val COMBO_TIMEOUT = 3000L // 3秒超时

    @SubscribeEvent
    fun onSkillCast(event: OrryxPlayerSkillCastEvents.Cast) {
        val player = event.player
        val uuid = player.uniqueId
        val currentTime = System.currentTimeMillis()

        // 检查连击是否超时
        val lastTime = lastCastTime[uuid] ?: 0L
        if (currentTime - lastTime > COMBO_TIMEOUT) {
            comboMap[uuid] = 0
        }

        // 增加连击数
        val combo = (comboMap[uuid] ?: 0) + 1
        comboMap[uuid] = combo
        lastCastTime[uuid] = currentTime

        // 显示连击数
        player.sendActionBar("连击: $combo")

        // 连击奖励
        if (combo >= 10) {
            player.sendMessage("10连击！伤害提升20%！")
        }
    }
}
```

**示例2：伤害统计系统**
```kotlin
object DamageStatistics {
    private val damageDealt = mutableMapOf<UUID, Double>()
    private val damageTaken = mutableMapOf<UUID, Double>()

    @SubscribeEvent
    fun onDamage(event: OrryxDamageEvents.Post) {
        val attacker = event.attacker
        val victim = event.victim
        val damage = event.damage

        // 记录造成的伤害
        if (attacker is Player) {
            val uuid = attacker.uniqueId
            damageDealt[uuid] = (damageDealt[uuid] ?: 0.0) + damage
        }

        // 记录受到的伤害
        if (victim is Player) {
            val uuid = victim.uniqueId
            damageTaken[uuid] = (damageTaken[uuid] ?: 0.0) + damage
        }
    }

    fun getPlayerStats(player: Player): Pair<Double, Double> {
        val uuid = player.uniqueId
        return Pair(
            damageDealt[uuid] ?: 0.0,
            damageTaken[uuid] ?: 0.0
        )
    }
}
```

---

## 6. 其他API模块

### 6.1 IProfileAPI - 玩家档案

玩家档案 API 管理玩家的状态（霸体、无敌、格挡、沉默等）。

```kotlin
interface IProfileAPI {
    // 获取玩家档案并操作
    fun <T> modifyProfile(player: Player, function: Function<IPlayerProfile, T>): CompletableFuture<T?>

    // 霸体状态管理
    fun isSuperBody(player: Player): Boolean
    fun superBodyCountdown(player: Player): Long
    fun setSuperBody(player: Player, timeout: Long)
    fun cancelSuperBody(player: Player)
    fun addSuperBody(player: Player, timeout: Long)
    fun reduceSuperBody(player: Player, timeout: Long)

    // 无敌状态管理
    fun isInvincible(player: Player): Boolean
    fun invincibleCountdown(player: Player): Long
    fun setInvincible(player: Player, timeout: Long)
    fun cancelInvincible(player: Player)
    fun addInvincible(player: Player, timeout: Long)
    fun reduceInvincible(player: Player, timeout: Long)

    // 免疫摔伤管理
    fun isSuperFoot(player: Player): Boolean
    fun setSuperFoot(player: Player, timeout: Long)
    fun cancelSuperFoot(player: Player)

    // 格挡状态管理（按伤害类型）
    fun isBlock(player: Player, blockType: DamageType): Boolean
    fun setBlock(player: Player, blockType: DamageType, timeout: Long, success: Consumer<OrryxDamageEvents.Pre>)
    fun cancelBlock(player: Player, blockType: DamageType)
    fun cancelBlock(player: Player)  // 取消所有格挡

    // 沉默状态管理
    fun isSilence(player: Player): Boolean
    fun silenceCountdown(player: Player): Long
    fun setSilence(player: Player, timeout: Long)
    fun cancelSilence(player: Player)
    fun addSilence(player: Player, timeout: Long)
    fun reduceSilence(player: Player, timeout: Long)
}
```

**使用示例：**
```kotlin
val api = Orryx.api()

// 设置玩家霸体 5 秒
api.profileAPI.setSuperBody(player, 5000)

// 设置玩家无敌 3 秒
api.profileAPI.setInvincible(player, 3000)

// 设置格挡物理伤害 2 秒
api.profileAPI.setBlock(player, DamageType.PHYSICAL, 2000) { damageEvent ->
    // 格挡成功时的回调
    player.sendMessage("成功格挡了 ${damageEvent.damage} 点物理伤害！")
}

// 沉默玩家 10 秒（无法释放技能）
api.profileAPI.setSilence(player, 10000)

// 检查状态
if (api.profileAPI.isSuperBody(player)) {
    player.sendMessage("你处于霸体状态，剩余 ${api.profileAPI.superBodyCountdown(player)}ms")
}

// 操作玩家档案（技能点、Flag等）
api.profileAPI.modifyProfile(player) { profile ->
    // 获取技能点
    val points = profile.point
    player.sendMessage("当前技能点: $points")

    // 设置 Flag
    profile.setFlag("custom_data", "value")

    // 获取 Flag
    val data = profile.getFlag<String>("custom_data")

    // 保存更改
    profile.save()
}
```

### 6.2 IKeyAPI - 按键绑定

```kotlin
val api = Orryx.api()

// 获取技能组
val group = api.keyAPI.getGroup("default")

// 获取绑定按键
val bindKey = api.keyAPI.getBindKey("F")

// 为技能组绑定技能到按键
api.keyAPI.bindSkillKeyOfGroup(player, "warrior", group, bindKey, "fireball")

// 修改玩家按键设置
api.keyAPI.modifyKeySetting(player) { keySetting ->
    // 操作按键设置
    keySetting.save()
}
```

### 6.3 ITaskAPI - 任务管理

```kotlin
val api = Orryx.api()

// 创建简单任务
api.taskAPI.createSimpleTask(player, "task_key") {
    // 任务逻辑
    player.sendMessage("任务执行中...")
}

// 创建管道任务（链式任务）
api.taskAPI.pipeBuilder(player, "pipe_key")
    .then { /* 第一步 */ }
    .then { /* 第二步 */ }
    .then { /* 第三步 */ }
    .build()

// 完成管道任务
api.taskAPI.completePipeTask(player, "pipe_key")
```

### 6.4 ITimerAPI - 计时器

```kotlin
val api = Orryx.api()

// 获取技能计时器
val skillTimer = api.timerAPI.getSkillTimer(player, "fireball")
skillTimer?.let { timer ->
    if (timer.hasCountdown()) {
        player.sendMessage("技能冷却中: ${timer.countdown}ms")
    }
}

// 获取中转站计时器
val stationTimer = api.timerAPI.getStationTimer(player, "station_key")
```

### 6.5 IConsumptionValueAPI - 法力/精力

```kotlin
val api = Orryx.api()

// 获取法力值管理器
val manaManager = api.consumptionValueAPI.manaInstance

// 法力值操作
manaManager.give(player, 100.0)   // 给予法力值
manaManager.take(player, 50.0)   // 消耗法力值
manaManager.set(player, 200.0)   // 设置法力值
val currentMana = manaManager.get(player)  // 获取当前法力值
val maxMana = manaManager.getMax(player)   // 获取最大法力值
val isEnough = manaManager.isEnough(player, 100.0)  // 检查是否足够

// 获取精力值管理器
val spiritManager = api.consumptionValueAPI.spiritInstance

// 精力值操作（与法力值类似）
spiritManager.give(player, 50.0)
spiritManager.take(player, 25.0)
```

### 6.6 碰撞系统 API

Orryx 提供了 8 种碰撞箱类型，用于技能范围检测。

**碰撞箱类型：**
| 类型 | 接口 | 说明 |
|------|------|------|
| Sphere | ISphere | 球体碰撞箱 |
| Capsule | ICapsule | 胶囊体碰撞箱 |
| AABB | IAABB | 轴对齐包围盒 |
| OBB | IOBB | 有向包围盒 |
| Ray | IRay | 射线碰撞 |
| Composite | IComposite | 复合碰撞箱 |
| Local* | ILocal* | 局部坐标系碰撞箱 |

**碰撞检测示例：**
```kotlin
// 碰撞箱通常在 Kether 脚本中使用
// 以下是概念示例

// 球体碰撞检测
val sphere: ISphere = // 创建球体
val entities = sphere.getEntitiesInside(world)

// 射线碰撞检测
val ray: IRay = // 创建射线
val hitResult = ray.trace(world, maxDistance)
```

---

## 7. 脚本引擎

### 7.1 Kether 脚本基础

Kether 是 Orryx 的主要脚本引擎，提供 40+ 内置动作。

**动作分类：**
- **基础动作**：延迟、同步、条件判断、流程控制
- **技能动作**：冷却管理、法力/精力操作
- **效果动作**：粒子特效、动画、音效
- **数学动作**：矩阵变换、四元数、向量运算
- **选择器动作**：几何体范围选择、目标筛选
- **射线动作**：光线追踪、碰撞检测

### 7.2 Kotlin 脚本（热重载）

Orryx 支持 Kotlin 脚本（.kts 文件），具有热重载能力。

**脚本位置：** `plugins/Orryx/kts/`

**脚本示例：**
```kotlin
// plugins/Orryx/kts/my_script.kts

import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx

// 脚本入口
fun execute(player: Player) {
    val api = Orryx.api()

    // 使用 API
    api.skillAPI.castSkill(player, "fireball", 1)

    player.sendMessage("脚本执行完成")
}
```

### 7.3 脚本执行 API

```kotlin
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.kts.ScriptManager

// 加载 Kether 脚本
val script = OrryxAPI.ketherScriptLoader.load("""
    delay 20
    message "Hello World"
""")

// 执行脚本
ScriptManager.runScript(player, parameter, script)
```

---

## 8. 扩展开发

### 8.1 自定义技能类型

要创建自定义技能类型，需要继承相应的基类：

```kotlin
// 自定义技能加载器
class MyCustomSkillLoader : AbstractCastSkillLoader() {

    override fun load(config: ConfigurationSection): ICastSkill {
        // 从配置加载技能
        return MyCustomSkill(config)
    }
}

// 自定义技能实现
class MyCustomSkill(config: ConfigurationSection) : ICastSkill {
    // 实现技能逻辑
}
```

### 8.2 自定义触发器

```kotlin
// 实现 ITrigger 接口
class MyCustomTrigger : ITrigger {

    override val key: String = "my_custom_trigger"

    override fun register() {
        // 注册触发器逻辑
    }

    override fun unregister() {
        // 注销触发器逻辑
    }
}

// 在 OrryxTriggerRegisterEvent 中注册
@SubscribeEvent
fun onTriggerRegister(event: OrryxTriggerRegisterEvent) {
    event.register(MyCustomTrigger())
}
```

### 8.3 自定义选择器

```kotlin
// 实现选择器接口
class MyCustomSelector : ISelector {

    override fun select(origin: Location, parameter: IParameter): List<IEntity> {
        // 实现选择逻辑
        return emptyList()
    }
}
```

### 8.4 第三方插件集成

**监听第三方插件事件：**
```kotlin
// DragonCore 按键事件
@SubscribeEvent
fun onDragonCoreKey(event: DragonCoreKeyEvent) {
    val player = event.player
    val key = event.key
    // 处理按键
}

// GermPlugin 客户端连接
@SubscribeEvent
fun onGermConnect(event: GermClientConnectEvent) {
    val player = event.player
    // 处理客户端连接
}

// DungeonPlus 副本事件
@SubscribeEvent
fun onDungeonStart(event: PlayerDungeonEvent.Start) {
    val player = event.player
    val dungeon = event.dungeon
    // 处理副本开始
}
```

---

## 9. 附录

### 9.1 完整API索引

| API | 接口 | 说明 |
|-----|------|------|
| 主入口 | IOrryxAPI | 提供所有子 API 的访问 |
| 技能 | ISkillAPI | 技能获取、修改、释放 |
| 职业 | IJobAPI | 职业数据管理 |
| 档案 | IProfileAPI | 玩家状态管理 |
| 按键 | IKeyAPI | 按键绑定管理 |
| 任务 | ITaskAPI | 任务创建和管理 |
| 计时器 | ITimerAPI | 冷却计时器 |
| 资源 | IConsumptionValueAPI | 法力/精力管理 |
| 杂项 | IMiscAPI | 经验计算器等 |
| 重载 | IReloadAPI | 配置重载 |

### 9.2 常见问题FAQ

**Q: 如何获取 API 实例？**
```kotlin
val api = Orryx.api()
```

**Q: 为什么 modifySkill/modifyJob 返回 CompletableFuture？**
A: Orryx 的数据操作是异步的，使用 CompletableFuture 可以避免阻塞主线程。

**Q: 修改后的数据如何保存？**
A: 调用对应对象的 `save()` 方法，如 `skill.save()`、`playerJob.save()`。

**Q: 如何监听事件？**
A: 使用 TabooLib 的 `@SubscribeEvent` 注解：
```kotlin
@SubscribeEvent
fun onEvent(event: SomeEvent) {
    // 处理事件
}
```

**Q: 如何取消事件？**
A: 对于可取消的事件，设置 `event.isCancelled = true`。

### 9.3 更新日志

请参阅项目的 [GitHub Releases](https://github.com/zhibeigg/Orryx/releases) 获取完整的更新日志。

---

## 相关资源

- [飞书 Wiki](https://o0vvjwgpeju.feishu.cn/wiki/Syzzw7aQwixJ4YkXoOAcyYkfnOg) - 完整使用文档
- [DeepWiki AI](https://deepwiki.com/zhibeigg/Orryx) - AI 问答助手
- [ZRead AI](https://zread.ai/zhibeigg/Orryx) - AI 问答助手
- [GitHub 仓库](https://github.com/zhibeigg/Orryx) - 源代码

---

*文档版本: 1.34.88*
*最后更新: 2026-01-27*
