package org.gitee.orryx.core.kether.actions.game.projectile

import org.bukkit.block.Block
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.api.adapters.IVector
import org.gitee.orryx.api.collider.local.ILocalCollider
import org.gitee.orryx.core.kether.actions.game.projectile.Projectile.ProjectileType.*
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.basic.AABBPlus
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.utils.*
import org.joml.Vector3d
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import taboolib.common.platform.service.PlatformExecutor
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestContext
import taboolib.module.kether.run
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

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

    private val lifecycle = SingleFlightLifecycle()
    private val started = AtomicBoolean(false)
    private val lifetime = ProjectileLifetime(period, timeout)
    private var task: PlatformExecutor.PlatformTask? = null
    private var currentCycle: CompletableFuture<*>? = null
    private val activeActions = ConcurrentHashMap.newKeySet<CompletableFuture<*>>()
    private val ticked: Long
        get() = lifetime.ticked
    private var hitCount = 0
    private val hitCountMap = HashMap<UUID, Int>()
    private val entityBoundsCenter = Vector3d()
    private val entityBoundsExtents = Vector3d()
    private val entityBounds = AABBPlus<ITargetLocation<*>>(entityBoundsExtents, entityBoundsCenter)
    private var warnedUnsupportedBlockCollision = false

    fun start(frame: QuestContext.Frame) {
        if (!started.compareAndSet(false, true)) return
        ensureSync {
            if (!lifecycle.isActive) return@ensureSync
            source.getBukkitLivingEntity()?.let { entity ->
                entity.setGravity(true)
                val potionDuration = timeout.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
                entity.addPotionEffect(PotionEffect(PotionEffectType.LEVITATION, potionDuration, -1, false, false))
            }
            task = submit(delay = lifetime.period, period = lifetime.period) {
                nextTick(frame)
            }
            nextTick(frame)
        }
    }

    fun validCheck() {
        if (!source.isValid) remove()
    }

    fun nextTick(frame: QuestContext.Frame) {
        if (!lifecycle.isActive) return
        if (!source.isValid || !lifetime.advance()) {
            remove()
            return
        }
        if (!lifecycle.tryStart()) return

        frame.variables().set("@ticked", ticked)
        currentCycle = invokeAction(frame, vector).vector { it }
            .thenApplyMain { nextVector ->
                if (!lifecycle.isActive || !source.isValid) return@thenApplyMain false
                lookAndMove(nextVector)
                hitbox.update()
                if (isSyncClient) syncClient()
                true
            }.thenCompose { moved ->
                if (!moved || !lifecycle.isActive) {
                    CompletableFuture.completedFuture(null)
                } else {
                    onPeriod?.let { invokeAction(frame, it) } ?: CompletableFuture.completedFuture(null)
                }
            }.handle { _, throwable ->
                throwable?.printStackTrace()
                Unit
            }.thenApplyMain {
                if (!lifecycle.isActive || !source.isValid) return@thenApplyMain null
                checkHitBlock {}
                findHitEntity()
            }.thenComposeMain { entity ->
                if (entity == null || !lifecycle.isActive) {
                    CompletableFuture.completedFuture(null)
                } else {
                    recordHit(frame, entity)
                    onHit?.let { invokeAction(frame, it) } ?: CompletableFuture.completedFuture(null)
                }
            }.whenComplete { _, throwable ->
                try {
                    runOnMainThread {
                        try {
                            if (throwable != null) {
                                throwable.printStackTrace()
                                remove()
                            } else if (lifecycle.isActive && !source.isValid) {
                                remove()
                            }
                        } finally {
                            lifecycle.finish()
                        }
                    }
                } catch (scheduleFailure: Throwable) {
                    lifecycle.finish()
                    scheduleFailure.printStackTrace()
                    remove()
                }
            }
    }

    private fun invokeAction(frame: QuestContext.Frame, action: ParsedAction<*>): CompletableFuture<Any?> {
        val actionFuture = invokeFuture { frame.run(action) }
        if (!lifecycle.isActive) {
            actionFuture.cancel(true)
            return actionFuture
        }
        activeActions += actionFuture
        actionFuture.whenComplete { _, _ -> activeActions.remove(actionFuture) }
        return actionFuture
    }

    private fun <R> invokeFuture(block: () -> CompletionStage<R>): CompletableFuture<R> {
        return try {
            block().toCompletableFuture()
        } catch (throwable: Throwable) {
            CompletableFuture<R>().also { it.completeExceptionally(throwable) }
        }
    }

    fun lookAndMove(vector: IVector) {
        if (!lifecycle.isActive || !source.isValid) return
        val movement = vector.bukkit()
        val current = source.location
        val face = if (movement.lengthSquared() > 1e-12) movement.clone().normalize() else current.direction
        if (through) {
            source.teleport(current.clone().add(movement).setDirection(face))
        } else {
            source.teleport(current.clone().setDirection(face))
            source.velocity = movement
        }
    }

    fun checkHitBlock(onHit: (Block) -> Unit): Boolean {
        if (hitBlock && !warnedUnsupportedBlockCollision) {
            warnedUnsupportedBlockCollision = true
            warning("暂不支持与方块碰撞")
        }
        return false
    }

    fun checkHitEntity(onHit: (IEntity) -> Unit): Boolean {
        val entity = findHitEntity() ?: return false
        onHit(entity)
        return true
    }

    private fun findHitEntity(): IEntity? {
        if (!hitEntity || !lifecycle.isActive || !source.isValid) return null
        val broadPhase = hitbox.fastCollider ?: return null
        val center = broadPhase.center
        val halfExtents = broadPhase.halfExtents
        val queryCenter = source.location.clone().apply {
            x = center.x
            y = center.y
            z = center.z
        }
        return source.world.getNearbyEntities(
            queryCenter,
            halfExtents.x.coerceAtLeast(0.0),
            halfExtents.y.coerceAtLeast(0.0),
            halfExtents.z.coerceAtLeast(0.0),
        ).firstNotNullOfOrNull { entity ->
            if (entity.uniqueId == source.uniqueId || !entity.isValid || entity.isDead) return@firstNotNullOfOrNull null
            val entityLocation = entity.location
            entityBoundsCenter.set(entityLocation.x, entityLocation.y + entity.height / 2.0, entityLocation.z)
            entityBoundsExtents.set(entity.width / 2.0, entity.height / 2.0, entity.width / 2.0)
            if (colliding(hitbox, entityBounds)) entity.abstract() else null
        }
    }

    private fun recordHit(frame: QuestContext.Frame, entity: IEntity) {
        hitCount++
        val entityHitCount = hitCountMap.getOrElse(entity.uniqueId) { 0 } + 1
        hitCountMap[entity.uniqueId] = entityHitCount
        frame.variables().set("@hitEntity", entity)
        frame.variables().set("hitCount", hitCount)
        frame.variables().set("entityHitCount", entityHitCount)
    }

    fun hit(frame: QuestContext.Frame) {
        if (!lifecycle.isActive) return
        hitCount++
        frame.variables().set("hitCount", hitCount)
        onHit?.also { invokeAction(frame, it) }
    }

    fun syncClient() {
        warning("syncClient 尚未实现，暂不支持同步投射物到客户端引擎")
    }

    fun remove() {
        if (!lifecycle.close()) return
        removed.complete(true)
        task?.cancel()
        currentCycle?.cancel(true)
        currentCycle = null
        activeActions.toList().forEach { it.cancel(true) }
        activeActions.clear()
        ensureSync {
            if (source.isValid) source.remove()
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
