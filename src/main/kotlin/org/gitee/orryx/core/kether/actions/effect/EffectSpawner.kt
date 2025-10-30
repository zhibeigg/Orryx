package org.gitee.orryx.core.kether.actions.effect

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.material.MaterialData
import org.gitee.orryx.api.OrryxAPI.Companion.effectScope
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.kether.actions.effect.EffectType.*
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.utils.*
import org.joml.Matrix3d
import org.joml.Vector3d
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.util.Location
import taboolib.common5.cdouble
import taboolib.module.effect.*
import taboolib.module.effect.shape.NStar
import taboolib.module.effect.shape.OctagonalStar
import taboolib.module.effect.shape.Pyramid
import taboolib.module.effect.shape.Ray.RayStopType
import taboolib.platform.util.toBukkitLocation
import java.util.concurrent.CompletableFuture

class EffectSpawner(val builder: EffectBuilder, val duration: Long = 1, val tick: Long = 1, val mode: SpawnerType = SpawnerType.PLAY, origins: IContainer, val viewers: IContainer): ParticleSpawner {

    val future = CompletableFuture<Void>()

    private val effects =
        origins.mapInstance<ITargetLocation<*>, OrryxParticleObj> {
            build(EffectOrigin(it))
        }

    fun start() {
        effectScope.launch {
            effects.map { effect ->
                async {
                    effect.start()
                    effect.future.join()
                }
            }.awaitAll()
        }.invokeOnCompletion {
            future.complete(null)
        }
    }

    fun stop() {
        effects.forEach {
            it.stop()
        }
    }

    override fun spawn(location: Location) {
        viewers.forEachInstance<ITargetEntity<Player>> { target ->
            adaptPlayer(target.getSource()).sendParticle(
                builder.particle.name,
                location,
                taboolib.common.util.Vector(builder.offset.x(), builder.offset.y(), builder.offset.z()),
                builder.count,
                builder.speed,
                getParticleData()
            )
        }
    }

    private fun getParticleData(): Any? {
        return when (val data = builder.data) {
            // 渐变红石
            is ParticleData.DustTransitionData -> {
                Particle.DustTransition(
                    Color.fromRGB(data.color.red, data.color.green, data.color.blue),
                    Color.fromRGB(data.toColor.red, data.toColor.blue, data.toColor.green),
                    data.size
                )
            }
            // 红石
            is ParticleData.DustData -> {
                Particle.DustOptions(Color.fromRGB(data.color.red, data.color.green, data.color.blue), data.size)
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
                } else {
                    Material.valueOf(data.material).createBlockData()
                }
            }
            // 震动（不知道怎么翻译，来自 1.17+）
            is ParticleData.VibrationData -> {
                Vibration(
                    data.origin.toBukkitLocation(), when (val destination = data.destination) {
                        // 坐标
                        is ParticleData.VibrationData.LocationDestination -> {
                            Vibration.Destination.BlockDestination(destination.location.toBukkitLocation())
                        }
                        // 实体
                        is ParticleData.VibrationData.EntityDestination -> {
                            Vibration.Destination.EntityDestination(Bukkit.getEntity(destination.entity)!!)
                        }
                    },
                    data.arrivalTime
                )
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
        val o = origin.getLocation(builder).toVector().joml()
        val width = builder.width
        val height = builder.height
        val length = builder.length
        val min = o.add(-0.5, -0.5, -0.5, Vector3d()).toLocation()
        val max = o.add(0.5, 0.5, 0.5, Vector3d())
        val z = origin.getLocation(builder).direction.clone().setY(0).normalize().crossProduct(taboolib.common.util.Vector(0, 1, 0)).joml()
        val matrix = Matrix3d().scale(width, height, length).rotateY(origin.getLocation(builder).yaw.cdouble).rotate(origin.getLocation(builder).pitch.cdouble, z)
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
        val locs = mutableListOf<Pair<Int, EffectOrigin>>()
        locs += 0 to origin
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