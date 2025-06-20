package org.gitee.orryx.core.kether.actions.game.projectile

import org.bukkit.block.Block
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.api.adapters.IVector
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.api.collider.local.ILocalCollider
import org.gitee.orryx.core.kether.actions.game.projectile.Projectile.ProjectileType.*
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.local.LocalAABB
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.utils.*
import org.joml.Vector3d
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.function.warning
import taboolib.common.platform.service.PlatformExecutor
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestContext
import taboolib.module.kether.run
import java.util.concurrent.CompletableFuture

class Projectile<T: ITargetLocation<*>>(
    val type: ProjectileType,
    val period: Long,
    val timeout: Long,
    parent: T,
    val hitbox: ILocalCollider<*>,
    val vector: ParsedAction<*>,
    val onHit: ParsedAction<*>?,
    val onPeriod: ParsedAction<*>?,
    val hitBlock: Boolean,
    val hitEntity: Boolean,
    val through: Boolean
) {

    var isSyncClient = false

    val source: IEntity = parent as IEntity

    val removed = CompletableFuture<Boolean>()

    lateinit var task: PlatformExecutor.PlatformTask
    private var ticked = -period
    private var hitCount = 0

    fun start(frame: QuestContext.Frame) {
        ensureSync {
            val entity = source.getBukkitLivingEntity() ?: return@ensureSync
            entity.setGravity(true)
            entity.addPotionEffect(PotionEffect(PotionEffectType.LEVITATION, timeout.toInt(), -1, false, false))
        }
        task = submitAsync(delay = period, period = period) {
            nextTick(frame)
        }
        nextTick(frame)
    }

    fun nextTick(frame: QuestContext.Frame) {
        ticked += period

        val periodFuture = CompletableFuture<Void>()
        val hitBlockFuture = CompletableFuture<Block>()
        val hitEntityFuture = CompletableFuture<IEntity>()

        periodFuture.thenAccept {
            if (isSyncClient) syncClient()
            onPeriod?.also { frame.run(it) }
        }

        hitBlockFuture.thenAccept {
            hitCount ++
            onHit?.also {
                frame.variables().set("@hitBlock", it)
                frame.run(it)
            }
        }

        hitEntityFuture.thenAccept {
            hitCount ++
            onHit?.also {
                frame.variables().set("@hitEntity", it)
                frame.run(it)
            }
        }

        frame.variables().set("@ticked", ticked)
        frame.run(vector).vector { vector ->
            when (type) {
                BUKKIT_ENTITY, BUKKIT_PROJECTILE -> ensureSync {
                    lookAndMove(vector)
                    hitbox.update()
                    periodFuture.complete(null)
                    if (!checkHitBlock { hitBlockFuture.complete(it) }) {
                        hitBlockFuture.cancel(true)
                    } else if (!checkHitEntity { hitEntityFuture.complete(it) }) {
                        hitEntityFuture.cancel(true)
                    }
                }
                ADY_ENTITY, NONE -> {
                    lookAndMove(vector)
                    hitbox.update()
                    periodFuture.complete(null)
                    ensureSync {
                        if (!checkHitBlock { hitBlockFuture.complete(it) }) {
                            hitBlockFuture.cancel(true)
                        } else if (!checkHitEntity { hitEntityFuture.complete(it) }) {
                            hitEntityFuture.cancel(true)
                        }
                    }
                }
            }
        }

        if (ticked >= timeout) {
            remove()
        }
    }

    fun lookAndMove(vector: IVector) {
        val face = vector.normalize(Vector3d()).bukkit()
        if (through) {
            source.teleport(source.location.clone().add(vector.bukkit()).setDirection(face))
        } else {
            source.teleport(source.location.clone().setDirection(face))
            source.velocity = vector.bukkit()
        }
    }

    fun checkHitBlock(onHit: (Block) -> Unit): Boolean {
        if (hitBlock) warning("暂不支持与方块碰撞")
        return false
    }

    fun checkHitEntity(onHit: (IEntity) -> Unit): Boolean {
        if (hitEntity) {
            val length = hitbox.fastCollider?.halfExtents?.length() ?: return false
            val entities = source.world.getNearbyEntities(source.location, length, length, length)
            entities.forEach {
                val abstract = it.abstract()
                val hitbox = LocalAABB<AbstractBukkitEntity>(
                    Vector3d(0.0, it.height/2, 0.0),
                    Vector3d(it.width/2, it.height/2,it.width/2),
                    abstract.coordinateConverter()
                    )
                if (colliding(this.hitbox, hitbox)) {
                    onHit(abstract)
                    return true
                }
            }
        }
        return false
    }

    fun hit(frame: QuestContext.Frame) {
        hitCount ++
        onHit?.also { frame.run(it) }
    }

    fun syncClient() {
        TODO()
    }

    fun remove() {
        if (removed.isDone) return
        removed.complete(true)
        task.cancel()
        ensureSync {
            if (source.isValid) {
                source.remove()
            }
        }
    }

    enum class ProjectileType {
        BUKKIT_ENTITY, ADY_ENTITY, BUKKIT_PROJECTILE, NONE;

        companion object {

            fun parseIgnoreCase(type: String): ProjectileType {
                return when (type.uppercase()) {
                    "ENTITY" -> BUKKIT_ENTITY
                    "ADY", "ADYESHACH" -> ADY_ENTITY
                    "BUKKIT" -> BUKKIT_PROJECTILE
                    "NONE" -> NONE
                    else -> error("Unknown projectile type: $type")
                }
            }
        }
    }

    override fun toString(): String {
        return "Projectile(" +
                "type=$type, " +
                "period=${period}, " +
                "timeout=${timeout}, " +
                "parent=$source, " +
                "hitbox=${hitbox}, " +
                "vector=${vector}, " +
                "hitBlock=$hitBlock, " +
                "hitEntity=$hitEntity" +
                ")"
    }
}