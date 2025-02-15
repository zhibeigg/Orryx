package org.gitee.orryx.core.kether.actions.game.entity

import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.entity.EntityTypes
import ink.ptms.adyeshach.core.entity.manager.ManagerType
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.api.adapters.IVector
import org.gitee.orryx.api.adapters.entity.AbstractAdyeshachEntity
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.utils.AdyeshachPlugin
import org.gitee.orryx.utils.bukkit
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.module.nms.spawnEntity
import taboolib.platform.util.setMeta
import java.util.*

class EntityBuilder {

    companion object {

        val taskMap by lazy { mutableMapOf<UUID, PlatformExecutor.PlatformTask>() }

    }

    private var name = ""
    private var type: EntityType = EntityType.ARMOR_STAND
    private var isPrivate = true

    private var timeout: Long = 0
    private var health: Double = 0.0
    private var gravity: Boolean = true
    private var vector: IVector? = null
    private lateinit var location: Location

    fun name(name: String): EntityBuilder {
        this.name = name
        return this
    }

    fun type(type: EntityType): EntityBuilder {
        this.type = type
        return this
    }

    fun private(isPrivate: Boolean): EntityBuilder {
        this.isPrivate = isPrivate
        return this
    }

    fun location(location: Location): EntityBuilder {
        this.location = location
        return this
    }

    fun timeout(timeout: Long): EntityBuilder {
        this.timeout = timeout
        return this
    }

    fun health(health: Double): EntityBuilder {
        this.health = health
        return this
    }

    fun gravity(gravity: Boolean): EntityBuilder {
        this.gravity = gravity
        return this
    }

    fun vector(vector: IVector): EntityBuilder {
        this.vector = vector
        return this
    }

    fun build(player: Player? = null, isAdy: Boolean = false): IEntity {
        if (!isPrimaryThread) error("请勿在异步线程中生成实体")
        val entity = if (isAdy) {
            if (AdyeshachPlugin.isEnabled) {
                createAdyEntity(player)
            } else {
                error("未发现 Adyeshach 无法生成Ady实体")
            }
        } else {
            createBukkitEntity()
        }
        if (timeout > 0) {
            taskMap[entity.uniqueId] = submit(delay = timeout) {
                if (entity.isValid) {
                    entity.remove()
                }
                taskMap.remove(entity.uniqueId)
            }
        }
        return entity
    }

    private fun createAdyEntity(player: Player?): AbstractAdyeshachEntity {
        val instance = if (isPrivate) {
            Adyeshach.api().getPrivateEntityManager(player ?: error("生成私有实体必须指定玩家"), ManagerType.TEMPORARY).create(EntityTypes.valueOf(type.name), location) {
                it.setTag("source", "Orryx")
                it.setNoGravity(!gravity)
                vector?.bukkit()?.let { vector -> it.setVelocity(vector) }
            }
        } else {
            Adyeshach.api().getPublicEntityManager(ManagerType.TEMPORARY).create(EntityTypes.valueOf(type.name), location) {
                it.setTag("source", "Orryx")
                it.setNoGravity(!gravity)
                vector?.bukkit()?.let { vector -> it.setVelocity(vector) }
            }
        }
        return AbstractAdyeshachEntity(instance)
    }

    private fun createBukkitEntity(): AbstractBukkitEntity {
        val entity = location.spawnEntity(EntityType.valueOf(type.name).entityClass ?: error("当前版本不支持此实体类型 ${type.name}")) { entity ->
            entity.setMeta("source", "Orryx")
            if (health > 0) { (entity as? LivingEntity)?.health = health }
            entity.setGravity(gravity)
            vector?.bukkit()?.let { entity.velocity = it }
        }
        return AbstractBukkitEntity(entity)
    }

}