package org.gitee.orryx.core.kether.actions.game

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.potion.PotionEffect
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Property
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.OpenResult
import taboolib.common5.cdouble
import taboolib.common5.cfloat
import taboolib.common5.cint
import taboolib.library.xseries.XPotion
import taboolib.module.kether.*
import taboolib.module.nms.MinecraftVersion

object GameActions {

    init {
        registerProperty(
            locationProperty(),
            Property.new("Game原版游戏", "Location", "orryx.location.operator")
                .description("Bukkit Location 对象，包含坐标、朝向和世界信息")
                .addEntry("x", Type.DOUBLE, "X 坐标", true)
                .addEntry("y", Type.DOUBLE, "Y 坐标", true)
                .addEntry("z", Type.DOUBLE, "Z 坐标", true)
                .addEntry("yaw", Type.FLOAT, "偏航角", true)
                .addEntry("pitch", Type.FLOAT, "俯仰角", true)
                .addEntry("world", Type.STRING, "世界名称")
                .addEntry("blockX", Type.INT, "方块 X 坐标")
                .addEntry("blockY", Type.INT, "方块 Y 坐标")
                .addEntry("blockZ", Type.INT, "方块 Z 坐标")
                .addEntry("direction", Type.VECTOR, "方向向量")
                .addEntry("length", Type.DOUBLE, "到原点距离"),
            Location::class.java
        )
        registerProperty(
            entityProperty(),
            Property.new("Game原版游戏", "IEntity", "orryx.entity.operator")
                .description("实体适配器对象，包含实体的基础信息")
                .addEntry("uniqueId", Type.STRING, "实体 UUID")
                .addEntry("entityId", Type.INT, "实体数值 ID")
                .addEntry("name", Type.STRING, "实体名称")
                .addEntry("customName", Type.STRING, "自定义名称", true)
                .addEntry("type", Type.STRING, "实体类型")
                .addEntry("isDead", Type.BOOLEAN, "是否死亡")
                .addEntry("isValid", Type.BOOLEAN, "是否有效")
                .addEntry("world", Type.STRING, "所在世界名")
                .addEntry("width", Type.DOUBLE, "实体宽度")
                .addEntry("height", Type.DOUBLE, "实体高度")
                .addEntry("location", Type.ANY, "实体位置 (Location)")
                .addEntry("eyeLocation", Type.ANY, "视线位置 (Location)")
                .addEntry("velocity", Type.VECTOR, "速度向量", true)
                .addEntry("gravity", Type.BOOLEAN, "是否受重力")
                .addEntry("moveSpeed", Type.DOUBLE, "移动速度")
                .addEntry("isOnGround", Type.BOOLEAN, "是否在地面")
                .addEntry("isFrozen", Type.BOOLEAN, "是否冻结")
                .addEntry("isFired", Type.BOOLEAN, "是否着火")
                .addEntry("isInsideVehicle", Type.BOOLEAN, "是否在载具内")
                .addEntry("isSilent", Type.BOOLEAN, "是否静默")
                .addEntry("isCustomNameVisible", Type.BOOLEAN, "自定义名称是否可见")
                .addEntry("isGlowing", Type.BOOLEAN, "是否发光")
                .addEntry("isInWater", Type.BOOLEAN, "是否在水中")
                .addEntry("isInvulnerable", Type.BOOLEAN, "是否无敌"),
            IEntity::class.java
        )
        registerProperty(
            blockProperty(),
            Property.new("Game原版游戏", "Block", "orryx.block.operator")
                .description("Bukkit Block 对象，包含方块的基础信息")
                .addEntry("type", Type.STRING, "方块类型名")
                .addEntry("x", Type.INT, "X 坐标")
                .addEntry("y", Type.INT, "Y 坐标")
                .addEntry("z", Type.INT, "Z 坐标")
                .addEntry("world", Type.STRING, "世界名")
                .addEntry("location", Type.ANY, "方块位置 (Location)")
                .addEntry("lightLevel", Type.INT, "光照等级")
                .addEntry("temperature", Type.DOUBLE, "温度")
                .addEntry("humidity", Type.DOUBLE, "湿度")
                .addEntry("isLiquid", Type.BOOLEAN, "是否液体")
                .addEntry("isSolid", Type.BOOLEAN, "是否固体")
                .addEntry("isEmpty", Type.BOOLEAN, "是否为空")
                .addEntry("isPassable", Type.BOOLEAN, "是否可通过"),
            Block::class.java
        )
    }

    @KetherParser(["sprint"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionSprint() = combinationParser(
        Action.new("Game原版游戏", "设置跑步状态", "sprint", true)
            .description("设置跑步状态")
            .addEntry("是否跑步", Type.BOOLEAN, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            bool(),
            theyContainer(true)
        ).apply(it) { isSprint, container ->
            future {
                ensureSync {
                    container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                        player.getSource().isSprinting = isSprint
                    }
                }
            }
        }
    }

    @KetherParser(["flyHeight"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionFlyHeight() = combinationParser(
        Action.new("Game原版游戏", "获得玩家离地面高度", "flyHeight", true)
            .description("获得玩家离地面高度")
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            theyContainer(true)
        ).apply(it) { container ->
            future {
                val player = container.orElse(self()).firstInstanceOrNull<PlayerTarget>() ?: return@future completedFuture(null)
                completedFuture(floor(player.getSource().location.clone(), 256.0).second)
            }
        }
    }

    @KetherParser(["specialTarget"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionSpecialTarget() = combinationParser(
        Action.new("Game原版游戏", "设置旁观者模式下的附着视角", "specialTarget", true)
            .description("设置旁观者模式下的附着视角")
            .addEntry("目标实体", Type.CONTAINER)
            .addContainerEntry("玩家", optional = true, default = "@self")
    ) {
        it.group(
            container(),
            theyContainer(false)
        ).apply(it) { target, container ->
            future {
                ensureSync {
                    container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                        target?.firstInstanceOrNull<ITargetEntity<Entity>>()?.getSource()
                            ?.let { entity -> player.getSource().spectatorTarget = entity }
                    }
                }
            }
        }
    }

    @KetherParser(["potion"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionPotion() = scriptParser(
        Action.new("Game原版游戏", "设置药水效果", "potion", true)
            .description("设置药水效果")
            .addEntry("设置标识符", Type.SYMBOL, false, head = "set")
            .addEntry("效果", Type.STRING)
            .addEntry("持续时间", Type.INT, false)
            .addEntry("等级", Type.INT, true, "1", "level")
            .addContainerEntry("玩家", optional = true, default = "@self"),
        Action.new("Game原版游戏", "删除药水效果", "potion", true)
            .description("删除药水效果")
            .addEntry("删除标识符", Type.SYMBOL, false, head = "remove/delete")
            .addEntry("效果", Type.STRING)
            .addContainerEntry("实体", optional = true, default = "@self"),
        Action.new("Game原版游戏", "清除所有药水效果", "potion", true)
            .description("清除所有药水效果")
            .addEntry("清除标识符", Type.SYMBOL, false, head = "clear")
            .addContainerEntry("实体", optional = true, default = "@self")
    ) {
        it.switch {
            case("set") {
                val effect = nextToken().uppercase()
                val duration = nextParsedAction()
                val level = nextHeadAction("level", def = 1)
                val they = nextTheyContainerOrNull()
                actionFuture { future ->
                    run(duration).int { duration ->
                        run(level).int { level ->
                            containerOrSelf(they) { container ->
                                ensureSync {
                                    val type = XPotion.matchXPotion(effect).orElse(XPotion.SPEED).potionEffectType
                                        ?: return@ensureSync future.complete(false)
                                    val potion = PotionEffect(type, duration, level, false, false)
                                    future.complete(
                                        container.mapNotNullInstance<ITargetEntity<Entity>, Boolean> { target ->
                                            target.entity.getBukkitLivingEntity()?.addPotionEffect(potion, true)
                                        }.all { b -> b }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            case("remove", "delete") {
                val effect = nextToken().uppercase()
                val they = nextTheyContainerOrNull()
                actionFuture { future ->
                    containerOrSelf(they) { container ->
                        ensureSync {
                            val type = XPotion.matchXPotion(effect).orElse(XPotion.SPEED).potionEffectType
                                ?: return@ensureSync future.complete(null)
                            container.forEachInstance<ITargetEntity<Entity>> { target ->
                                target.entity.getBukkitLivingEntity()?.removePotionEffect(type)
                            }
                            future.complete(null)
                        }
                    }
                }
            }
            case("clear") {
                val they = nextTheyContainerOrNull()
                actionFuture { future ->
                    containerOrSelf(they) { container ->
                        ensureSync {
                            container.forEachInstance<ITargetEntity<Entity>> { target ->
                                target.entity.getBukkitLivingEntity()?.apply {
                                    activePotionEffects.forEach { effect ->
                                        removePotionEffect(effect.type)
                                    }
                                }
                            }
                            future.complete(null)
                        }
                    }
                }
            }
        }
    }

    private fun locationProperty() = object : ScriptProperty<Location>("orryx.location.operator") {

        override fun read(instance: Location, key: String): OpenResult {
            return when (key) {
                "x" -> OpenResult.successful(instance.x)
                "y" -> OpenResult.successful(instance.y)
                "z" -> OpenResult.successful(instance.z)
                "yaw" -> OpenResult.successful(instance.yaw)
                "pitch" -> OpenResult.successful(instance.pitch)
                "world" -> OpenResult.successful(instance.world?.name)
                "blockX" -> OpenResult.successful(instance.blockX)
                "blockY" -> OpenResult.successful(instance.blockY)
                "blockZ" -> OpenResult.successful(instance.blockZ)
                "direction" -> {
                    val dir = instance.direction
                    OpenResult.successful(AbstractVector(dir.x, dir.y, dir.z))
                }
                "length" -> OpenResult.successful(instance.length())
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: Location, key: String, value: Any?): OpenResult {
            return when (key) {
                "x" -> {
                    instance.x = value.cdouble
                    OpenResult.successful()
                }
                "y" -> {
                    instance.y = value.cdouble
                    OpenResult.successful()
                }
                "z" -> {
                    instance.z = value.cdouble
                    OpenResult.successful()
                }
                "yaw" -> {
                    instance.yaw = value.cfloat
                    OpenResult.successful()
                }
                "pitch" -> {
                    instance.pitch = value.cfloat
                    OpenResult.successful()
                }
                else -> OpenResult.failed()
            }
        }
    }

    private fun entityProperty() = object : ScriptProperty<IEntity>("orryx.entity.operator") {

        override fun read(instance: IEntity, key: String): OpenResult {
            return when (key) {
                "uniqueId" -> OpenResult.successful(instance.uniqueId.toString())
                "entityId" -> OpenResult.successful(instance.entityId)
                "name" -> OpenResult.successful(instance.name)
                "customName" -> OpenResult.successful(instance.customName)
                "type" -> OpenResult.successful(instance.type)
                "isDead" -> OpenResult.successful(instance.isDead)
                "isValid" -> OpenResult.successful(instance.isValid)
                "world" -> OpenResult.successful(instance.world.name)
                "width" -> OpenResult.successful(instance.width)
                "height" -> OpenResult.successful(instance.height)
                "location" -> OpenResult.successful(instance.location)
                "eyeLocation" -> OpenResult.successful(instance.eyeLocation)
                "velocity" -> {
                    val v = instance.velocity
                    OpenResult.successful(AbstractVector(v.x, v.y, v.z))
                }
                "gravity" -> OpenResult.successful(instance.gravity)
                "moveSpeed" -> OpenResult.successful(instance.moveSpeed)
                "isOnGround" -> OpenResult.successful(instance.isOnGround)
                "isFrozen" -> OpenResult.successful(instance.isFrozen)
                "isFired" -> OpenResult.successful(instance.isFired)
                "isInsideVehicle" -> OpenResult.successful(instance.isInsideVehicle)
                "isSilent" -> OpenResult.successful(instance.isSilent)
                "isCustomNameVisible" -> OpenResult.successful(instance.isCustomNameVisible)
                "isGlowing" -> OpenResult.successful(instance.isGlowing)
                "isInWater" -> OpenResult.successful(instance.isInWater)
                "isInvulnerable" -> OpenResult.successful(instance.isInvulnerable)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: IEntity, key: String, value: Any?): OpenResult {
            return when (key) {
                "customName" -> {
                    ensureSync { instance.customName = value as? String }
                    OpenResult.successful()
                }
                "velocity" -> {
                    ensureSync {
                        val vec = value as? AbstractVector
                        if (vec != null) {
                            instance.velocity = vec.bukkit()
                        }
                    }
                    OpenResult.successful()
                }
                else -> OpenResult.failed()
            }
        }
    }

    private fun blockProperty() = object : ScriptProperty<Block>("orryx.block.operator") {

        override fun read(instance: Block, key: String): OpenResult {
            return when (key) {
                "type" -> OpenResult.successful(instance.type.name)
                "x" -> OpenResult.successful(instance.x)
                "y" -> OpenResult.successful(instance.y)
                "z" -> OpenResult.successful(instance.z)
                "world" -> OpenResult.successful(instance.world.name)
                "location" -> OpenResult.successful(instance.location)
                "lightLevel" -> OpenResult.successful(instance.lightLevel.toInt())
                "temperature" -> OpenResult.successful(instance.temperature)
                "humidity" -> OpenResult.successful(instance.humidity)
                "isLiquid" -> OpenResult.successful(instance.isLiquid)
                "isSolid" -> OpenResult.successful(instance.type.isSolid)
                "isEmpty" -> OpenResult.successful(instance.isEmpty)
                "isPassable" -> OpenResult.successful(
                    if (MinecraftVersion.isHigher(MinecraftVersion.V1_12)) instance.isPassable else false
                )
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: Block, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }
}