package org.gitee.orryx.core.kether.actions.effect

import org.bukkit.entity.Player
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.kether.actions.effect.EffectType.*
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.utils.*
import org.joml.Matrix3d
import org.joml.Vector3d
import taboolib.common.platform.ProxyParticle
import taboolib.common.platform.function.adaptLocation
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.util.Location
import taboolib.common5.cdouble
import taboolib.module.effect.*
import taboolib.module.effect.shape.NStar
import taboolib.module.effect.shape.OctagonalStar
import taboolib.module.effect.shape.Pyramid
import taboolib.module.effect.shape.Ray.RayStopType

class EffectSpawner(val builder: EffectBuilder, val duration: Long = 1, val tick: Long = 1, val origins: IContainer, val viewers: IContainer, val func: () -> Unit = {}): ParticleSpawner {

    private val effects =
        origins.mapInstance<ITargetLocation<*>, OrryxParticleObj> {
            build(EffectOrigin(it))
        }

    fun start() {
        effects.forEach { effect ->
            effect.start()
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
                builder.particle,
                location,
                taboolib.common.util.Vector(builder.offset.x(), builder.offset.y(), builder.offset.z()),
                builder.count,
                builder.speed,
                getParticleData()
            )
        }
    }

    private fun getParticleData(): ProxyParticle.Data? {
        return builder.dustData ?: builder.dustTransitionData ?: builder.itemData ?: builder.blockData ?: builder.vibrationData
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
            adaptLocation(origin.getLocation(builder)),
            builder.startAngle,
            builder.angle,
            builder.radius,
            builder.step,
            builder.period
        ), this)
    }

    private fun buildAstroid(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createAstroid(
            adaptLocation(origin.getLocation(builder)),
            builder.radius,
            builder.step,
            builder.period
        ), this)
    }

    private fun buildCircle(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createCircle(
            adaptLocation(origin.getLocation(builder)),
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
            adaptLocation(origin.getLocation(builder)),
            builder.radius,
            builder.sample,
            builder.period
        ), this)
    }

    private fun buildHeart(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createHeart(
            builder.xScaleRate,
            builder.yScaleRate,
            adaptLocation(origin.getLocation(builder)),
            builder.period
        ), this)
    }

    private fun buildLine(origin: EffectOrigin): OrryxParticleObj {
        val start = adaptLocation(origin.getLocation(builder))
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
            adaptLocation(origin.getLocation(builder)),
            builder.period
        ), this)
    }

    private fun buildNRankBezierCurve(origin: EffectOrigin): OrryxParticleObj {
        val locs = mutableListOf<Pair<Int, EffectOrigin>>()
        locs += 0 to origin
        locs.addAll(builder.locations)
        locs.sortBy { it.first }
        return OrryxParticleObj(origin, createNRankBezierCurve(
            locs.map { adaptLocation(it.second.getLocation(builder)) },
            builder.step,
            builder.period
        ), this)
    }

    private fun buildNStar(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, NStar(
            adaptLocation(origin.getLocation(builder)),
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
            adaptLocation(origin.getLocation(builder)),
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
            adaptLocation(origin.getLocation(builder)),
            builder.radius,
            builder.side,
            builder.step,
            builder.period
        ), this)
    }

    private fun buildPyramid(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, Pyramid(
            adaptLocation(origin.getLocation(builder)),
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
            adaptLocation(origin.getLocation(builder)),
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
            adaptLocation(origin.getLocation(builder)),
            builder.radius,
            builder.sample,
            builder.period
        ), this)
    }

    private fun buildStar(origin: EffectOrigin): OrryxParticleObj {
        return OrryxParticleObj(origin, createStar(
            adaptLocation(origin.getLocation(builder)),
            builder.radius,
            builder.step,
            builder.period
        ), this)
    }

}