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
import java.util.concurrent.CompletableFuture

class EntityBuilder {

    private var name = ""
    private var type: EntityType = EntityType.ARMOR_STAND
    private var isPrivate = true

    private var timeout: Long = 0
    private var health: Double = 0.0
    private var gravity: Boolean = true
    private var vector: IVector? = null

    val removed = CompletableFuture<Boolean>()
    lateinit var task: PlatformExecutor.PlatformTask

    init {
        removed.whenComplete { _, _ ->
            task.cancel()
        }
    }

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

    fun build(locations: List<Location>, players: List<Player> = emptyList(), isAdy: Boolean = false): List<IEntity> {
        if (!isPrimaryThread) error("请勿在异步线程中生成实体")
        val entities = if (isAdy) {
            if (AdyeshachPlugin.isEnabled) {
                createAdyEntity(locations, players)
            } else {
                error("未发现 Adyeshach 无法生成Ady实体")
            }
        } else {
            createBukkitEntity(locations)
        }
        if (timeout > 0) {
            task = submit(delay = timeout) {
                entities.forEach { entity ->
                    if (entity.isValid) {
                        entity.remove()
                    }
                }
                removed.complete(true)
            }
        }
        return entities
    }

    private fun createAdyEntity(locations: List<Location>, players: List<Player>): List<AbstractAdyeshachEntity> {
        val type = EntityTypes.valueOf(type.name)
        return locations.map { location ->
            val instance = if (isPrivate) {
                Adyeshach.api()
                    .getPrivateEntityManager(players.firstOrNull() ?: error("生成私有实体必须指定玩家"), ManagerType.TEMPORARY)
                    .create(type, location) {
                        it.setTag("source", "Orryx")
                        it.setNoGravity(!gravity)
                        it.setCustomName(name)
                        vector?.bukkit()?.let { vector -> it.setVelocity(vector) }
                    }
            } else {
                Adyeshach.api().getPublicEntityManager(ManagerType.TEMPORARY)
                    .create(type, location) {
                        it.setTag("source", "Orryx")
                        it.setNoGravity(!gravity)
                        it.setCustomName(name)
                        it.clearViewer()
                        players.forEach { player -> it.addViewer(player) }
                        vector?.bukkit()?.let { vector -> it.setVelocity(vector) }
                    }
            }
            AbstractAdyeshachEntity(instance)
        }
    }

    private fun createBukkitEntity(locations: List<Location>): List<AbstractBukkitEntity> {
        return locations.map { location ->
            val entity = location.spawnEntity(type.entityClass ?: error("当前版本不支持此实体类型 ${type.name}")) { entity ->
                entity.customName = name
                entity.setMeta("source", "Orryx")
                if (health > 0) {
                    (entity as? LivingEntity)?.maxHealth = health
                    (entity as? LivingEntity)?.health = health
                }
                entity.setGravity(gravity)
                vector?.bukkit()?.let { entity.velocity = it }
            }
            AbstractBukkitEntity(entity)
        }
    }

}