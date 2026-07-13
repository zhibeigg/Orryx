package org.gitee.orryx.core.kether.actions.effect

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.material.MaterialData
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.kether.actions.effect.EffectType.*
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.utils.*
import org.joml.Matrix3d
import org.joml.Vector3d
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.common.util.Location
import taboolib.common5.cdouble
import taboolib.module.effect.*
import taboolib.module.effect.shape.NStar
import taboolib.module.effect.shape.OctagonalStar
import taboolib.module.effect.shape.Pyramid
import taboolib.module.effect.shape.Ray.RayStopType
import taboolib.platform.util.toBukkitLocation
import taboolib.module.nms.MinecraftVersion
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

private object EffectPacketBudget {
    private var resetTask: PlatformExecutor.PlatformTask? = null
    private var remaining = 0

    fun ensureStarted() {
        if (resetTask != null) return
        remaining = configuredLimit()
        resetTask = submit(period = 1) { remaining = configuredLimit() }
    }

    fun tryConsume(): Boolean {
        if (remaining <= 0) return false
        remaining--
        return true
    }

    private fun configuredLimit(): Int {
        return Orryx.config.getInt("Effect.MaxPacketsPerTick", 20000).coerceAtLeast(0)
    }
}

class EffectSpawner(val builder: EffectBuilder, val duration: Long = 1, val tick: Long = 1, val mode: SpawnerType = SpawnerType.PLAY, val origins: IContainer, val viewers: IContainer): ParticleSpawner {

    val future = CompletableFuture<Void>()

    private val particleName = builder.particle.name
    private val particleOffset = taboolib.common.util.Vector(builder.offset.x(), builder.offset.y(), builder.offset.z())
    private var particleData: Any? = null
    internal val matrix = builder.matrix?.taboo()

    private val effects = mutableListOf<OrryxParticleObj>()
    private val started = AtomicBoolean(false)
    private val stopped = AtomicBoolean(false)
    private var updateTask: PlatformExecutor.PlatformTask? = null
    private var renderViewers = emptyList<Player>()

    fun start() {
        if (stopped.get() || !started.compareAndSet(false, true)) return
        runOnMainThread {
            if (stopped.get()) return@runOnMainThread
            try {
                EffectPacketBudget.ensureStarted()
                particleData = createParticleData()
                effects += origins.mapInstance<ITargetLocation<*>, OrryxParticleObj> { build(EffectOrigin(it)) }
                effects.forEach { it.initialize() }
                if (stopped.get()) return@runOnMainThread
                if (effects.isEmpty()) {
                    stop()
                    return@runOnMainThread
                }
                var elapsed = 0L
                updateTask = submit(period = 1) {
                    try {
                        if (stopped.get()) {
                            cancel()
                            return@submit
                        }
                        renderViewers = viewers.mapInstance<ITargetEntity<Player>, Player> { it.getSource() }
                            .filter { it.isOnline }
                        if (elapsed % tick.coerceAtLeast(1L) == 0L) effects.forEach { it.sync() }
                        if (elapsed % builder.period.coerceAtLeast(1L) == 0L) effects.forEach { it.renderFrame() }
                        elapsed++
                        if (elapsed >= duration.coerceAtLeast(1L)) stop()
                    } catch (throwable: Throwable) {
                        stop(throwable)
                    }
                }
            } catch (throwable: Throwable) {
                stop(throwable)
            }
        }
    }

    fun stop() {
        stop(null)
    }

    private fun stop(throwable: Throwable?) {
        if (!stopped.compareAndSet(false, true)) return
        runOnMainThread {
            updateTask?.cancel()
            updateTask = null
            effects.forEach { it.stop() }
            renderViewers = emptyList()
            if (throwable == null) future.complete(null) else future.completeExceptionally(throwable)
        }
    }

    override fun spawn(location: Location) {
        val count = builder.count
        val speed = builder.speed
        for (viewer in renderViewers) {
            if (!EffectPacketBudget.tryConsume()) break
            adaptPlayer(viewer).sendParticle(
                particleName,
                location,
                particleOffset,
                count,
                speed,
                particleData
            )
        }
    }

    private fun createParticleData(): Any? {
        return when (val data = builder.data) {
            // 渐变红石（1.17+）
            is ParticleData.DustTransitionData -> {
                if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17)) {
                    Particle.DustTransition(
                        Color.fromRGB(data.color.red, data.color.green, data.color.blue),
                        Color.fromRGB(data.toColor.red, data.toColor.green, data.toColor.blue),
                        data.size
                    )
                } else if (MinecraftVersion.isHigher(MinecraftVersion.V1_12)) {
                    // 1.13-1.16 降级为普通 DustOptions
                    Particle.DustOptions(Color.fromRGB(data.color.red, data.color.green, data.color.blue), data.size)
                } else {
                    null
                }
            }
            // 红石（1.13+）
            is ParticleData.DustData -> {
                if (MinecraftVersion.isHigher(MinecraftVersion.V1_12)) {
                    Particle.DustOptions(Color.fromRGB(data.color.red, data.color.green, data.color.blue), data.size)
                } else {
                    null
                }
            }
            // 物品
            is ParticleData.ItemData -> {
                val item = ItemStack(Material.valueOf(data.material))
                val itemMeta = item.itemMeta!!
                itemMeta.setDisplayName(data.name)
                itemMeta.lore = data.lore
                try {
                    itemMeta.setCustomModelData(data.customModelData)
                } catch (_: NoSuchMethodError) {
                }
                item.itemMeta = itemMeta
                if (data.data != 0) {
                    item.durability = data.data.toShort()
                }
                item
            }
            // 方块
            is ParticleData.BlockData -> {
                if (builder.particle.get()?.dataType == MaterialData::class.java) {
                    MaterialData(Material.valueOf(data.material), data.data.toByte())
                } else if (MinecraftVersion.isHigher(MinecraftVersion.V1_12)) {
                    Material.valueOf(data.material).createBlockData()
                } else {
                    MaterialData(Material.valueOf(data.material), data.data.toByte())
                }
            }
            // 震动（1.17+）
            is ParticleData.VibrationData -> {
                if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17)) {
                    Vibration(
                        data.origin.toBukkitLocation(), when (val destination = data.destination) {
                            is ParticleData.VibrationData.LocationDestination -> {
                                Vibration.Destination.BlockDestination(destination.location.toBukkitLocation())
                            }
                            is ParticleData.VibrationData.EntityDestination -> {
                                val entity = Bukkit.getEntity(destination.entity) ?: return null
                                Vibration.Destination.EntityDestination(entity)
                            }
                        },
                        data.arrivalTime
                    )
                } else {
                    null
                }
            }
            else -> null
        }
    }

    fun build(origin: EffectOrigin): OrryxParticleObj {
        return when(builder.type) {
            ARC -> buildArc(origin)
            ASTROID -> buildAstroid(origin)
            CIRCLE -> buildCircle(origin)
            CUBE -> buildCube(origin)
            FILLED_CIRCLE -> buildFilledCircle(origin)
            HEART -> buildHeart(origin)
            LINE -> buildLine(origin)
            LOTUS -> buildLotus(origin)
            N_RANK_BEZIER_CURVE -> buildNRankBezierCurve(origin)
            N_STAR -> buildNStar(origin)
            OCTAGONAL_STAR -> buildOctagonalStar(origin)
            POLYGON -> buildPolygon(origin)
            PYRAMID -> buildPyramid(origin)
            RAY -> buildRay(origin)
            SPHERE -> buildSphere(origin)
            STAR -> buildStar(origin)
            WING -> error("")
        }
    }

    private fun buildArc(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createArc(
            origin.getLocation(builder),
            builder.startAngle,
            builder.angle,
            builder.radius,
            builder.step,
            builder.period
        ), this)
    }

    private fun buildAstroid(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createAstroid(
            origin.getLocation(builder),
            builder.radius,
            builder.step,
            builder.period
        ), this)
    }

    private fun buildCircle(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createCircle(
            origin.getLocation(builder),
            builder.radius,
            builder.step,
            builder.period
        ), this)
    }

    private fun buildCube(origin: EffectOrigin): OrryxParticleObj {
        val location = origin.getLocation(builder)
        val vector = location.toVector().joml()
        val min = vector.add(-0.5, -0.5, -0.5, Vector3d()).toLocation()
        val max = vector.add(0.5, 0.5, 0.5, Vector3d())
        val rotationAxis = location.direction.clone()
            .setY(0)
            .normalize()
            .crossProduct(taboolib.common.util.Vector(0, 1, 0))
            .joml()
        val matrix = Matrix3d()
            .scale(builder.width, builder.height, builder.length)
            .rotateY(location.yaw.cdouble)
            .rotate(location.pitch.cdouble, rotationAxis)
        return OrryxParticleObj(origin, createCube(
            Location(origin.bindTarget.world.name, min.x, min.y, min.z),
            Location(origin.bindTarget.world.name, max.x, max.y, max.z),
            builder.step,
            builder.period
        ).addMatrix(matrix.taboo()), this)
    }

    private fun buildFilledCircle(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createFilledCircle(
            origin.getLocation(builder),
            builder.radius,
            builder.sample,
            builder.period
        ), this)
    }

    private fun buildHeart(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createHeart(
            builder.xScaleRate,
            builder.yScaleRate,
            origin.getLocation(builder),
            builder.period
        ), this)
    }

    private fun buildLine(origin: EffectOrigin): OrryxParticleObj {
        val start = origin.getLocation(builder)
        val vector = builder.vector ?: error("粒子未设置Vector")
        val end = start.clone().add(vector.x(), vector.y(), vector.z())
        return OrryxParticleObj(origin, createLine(
            start,
            end,
            builder.step,
            builder.period
        ), this)
    }

    private fun buildLotus(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createLotus(
            origin.getLocation(builder),
            builder.period
        ), this)
    }

    private fun buildNRankBezierCurve(origin: EffectOrigin): OrryxParticleObj {
        val locs = mutableListOf<Tuple2<Int, EffectOrigin>>()
        locs += 0 paired origin
        locs.addAll(builder.locations)
        locs.sortBy { it.first }
        return OrryxParticleObj(origin, createNRankBezierCurve(
            locs.map { it.second.getLocation(builder) },
            builder.step,
            builder.period
        ), this)
    }

    private fun buildNStar(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, NStar(
            origin.getLocation(builder),
            builder.corner,
            builder.radius,
            builder.step,
            object : ParticleSpawner {
                override fun spawn(location: Location) {
                }
            }
        ), this)
    }

    private fun buildOctagonalStar(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, OctagonalStar(
            origin.getLocation(builder),
            builder.radius,
            builder.step,
            object : ParticleSpawner {
                override fun spawn(location: Location) {
                }
            }
        ), this)
    }

    private fun buildPolygon(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createPolygon(
            origin.getLocation(builder),
            builder.radius,
            builder.side,
            builder.step,
            builder.period
        ), this)
    }

    private fun buildPyramid(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, Pyramid(
            origin.getLocation(builder),
            builder.side,
            builder.radius,
            builder.height,
            builder.step,
            object : ParticleSpawner {
                override fun spawn(location: Location) {
                }
            }
        ), this)
    }

    private fun buildRay(origin: EffectOrigin): OrryxParticleObj {
        val vector = builder.vector ?: error("粒子未设置Vector")
        val direction = taboolib.common.util.Vector(vector.x(), vector.y(), vector.z())
        return OrryxParticleObj(origin, createRay(
            origin.getLocation(builder),
            direction,
            builder.maxLength,
            builder.step,
            builder.range,
            RayStopType.MAX_LENGTH,
            builder.period
        ), this)
    }

    private fun buildSphere(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createSphere(
            origin.getLocation(builder),
            builder.radius,
            builder.sample,
            builder.period
        ), this)
    }

    private fun buildStar(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createStar(
            origin.getLocation(builder),
            builder.radius,
            builder.step,
            builder.period
        ), this)
    }

}