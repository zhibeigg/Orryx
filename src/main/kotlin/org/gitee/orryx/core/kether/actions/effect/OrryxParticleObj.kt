package org.gitee.orryx.core.kether.actions.effect

import org.gitee.orryx.utils.joml
import org.gitee.orryx.utils.taboo
import org.gitee.orryx.utils.toLocation
import org.joml.Matrix3d
import org.joml.Vector3d
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.service.PlatformExecutor
import taboolib.common.util.Location
import taboolib.common5.cdouble
import taboolib.module.effect.ParticleObj
import taboolib.module.effect.Playable
import taboolib.module.effect.shape.*
import java.util.concurrent.CompletableFuture

class OrryxParticleObj(var effectOrigin: EffectOrigin, val obj: ParticleObj, val spawner: EffectSpawner) {

    var task: PlatformExecutor.PlatformTask? = null
    val future = CompletableFuture<Void>()

    fun start() {
        start(sharedTicker = false)
    }

    internal fun startManaged() {
        start(sharedTicker = true)
    }

    private fun start(sharedTicker: Boolean) {
        obj.spawner = spawner
        spawner.matrix?.let { obj.addMatrix(it) }
        if (spawner.duration <= 1L) {
            obj.show()
            future.complete(null)
        } else {
            if (obj is Playable && spawner.mode == SpawnerType.PLAY) {
                obj.alwaysPlayAsync()
            } else {
                obj.alwaysShowAsync()
            }
            if (!sharedTicker) {
                createTask()
            }
        }
    }

    private fun createTask() {
        var delay = 0L
        task = submitAsync(period = 1) {
            if (delay >= spawner.duration) stop()
            if (delay % spawner.tick.coerceAtLeast(1L) == 0L) {
                sync()
            }
            delay++
        }
    }

    internal fun sync() {
        when(obj) {
            is Arc -> syncArc()
            is Astroid -> syncAstroid()
            is Cube -> syncCube()
            is Heart -> syncHeart()
            is Line -> syncLine()
            is Lotus -> syncLotus()
            is NStar -> syncNStar()
            is OctagonalStar -> syncOctagonalStar()
            is Polygon -> syncPolygon()
            is Pyramid -> syncPyramid()
            is Ray -> syncRay()
            is Sphere -> syncSphere()
            is Star -> syncStar()
        }
        obj.period = spawner.builder.period
        spawner.matrix?.let { obj.setMatrix(it) }
    }

    fun stop() {
        task?.cancel()
        task = null
        obj.turnOffTask()
        submit(delay = 1) {
            future.complete(null)
        }
    }

    private fun syncArc() {
        (obj as Arc).also {
            it.origin = effectOrigin.getLocation(spawner.builder)
            it.startAngle = spawner.builder.startAngle
            it.angle = spawner.builder.angle
            it.radius = spawner.builder.radius
            it.step = spawner.builder.step
        }
    }

    private fun syncAstroid() {
        (obj as Astroid).also {
            it.origin = effectOrigin.getLocation(spawner.builder)
            it.radius = spawner.builder.radius
            it.step = spawner.builder.step
        }
    }

    private fun syncCube() {
        val location = effectOrigin.getLocation(spawner.builder)
        val origin = location.toVector().joml()
        val min = origin.add(-0.5, -0.5, -0.5, Vector3d()).toLocation()
        val max = origin.add(0.5, 0.5, 0.5, Vector3d())
        val rotationAxis = location.direction.clone()
            .setY(0)
            .normalize()
            .crossProduct(taboolib.common.util.Vector(0, 1, 0))
            .joml()
        val matrix = Matrix3d()
            .scale(spawner.builder.width, spawner.builder.height, spawner.builder.length)
            .rotateY(location.yaw.cdouble)
            .rotate(location.pitch.cdouble, rotationAxis)
        (obj as Cube).also {
            it.minLocation = Location(effectOrigin.bindTarget.world.name, min.x, min.y, min.z)
            it.maxLocation = Location(effectOrigin.bindTarget.world.name, max.x, max.y, max.z)
            it.step = spawner.builder.step
            it.setMatrix(matrix.taboo())
        }
    }

    private fun syncHeart() {
        (obj as Heart).also {
            it.origin = effectOrigin.getLocation(spawner.builder)
            it.xScaleRate = spawner.builder.xScaleRate
            it.yScaleRate = spawner.builder.yScaleRate
            it.step = spawner.builder.step
        }
    }

    private fun syncLine() {
        val start = effectOrigin.getLocation(spawner.builder)
        (obj as Line).also {
            it.start = start
            it.step = spawner.builder.step
            val vector = spawner.builder.vector ?: return
            val end = start.clone().add(vector.x(), vector.y(), vector.z())
            it.end = end
        }
    }

    private fun syncLotus() {
        (obj as Lotus).also {
            it.origin = effectOrigin.getLocation(spawner.builder)
        }
    }

    private fun syncNStar() {
        (obj as NStar).also {
            it.origin = effectOrigin.getLocation(spawner.builder)
            it.corner = spawner.builder.corner
            it.radius = spawner.builder.radius
            it.step = spawner.builder.step
        }
    }

    private fun syncOctagonalStar() {
        (obj as OctagonalStar).also {
            it.origin = effectOrigin.getLocation(spawner.builder)
            it.radius = spawner.builder.radius
            it.step = spawner.builder.step
        }
    }

    private fun syncPolygon() {
        (obj as Polygon).also {
            it.origin = effectOrigin.getLocation(spawner.builder)
            it.radius = spawner.builder.radius
            it.side = spawner.builder.side
            it.step = spawner.builder.step
        }
    }

    private fun syncPyramid() {
        (obj as Pyramid).also {
            it.origin = effectOrigin.getLocation(spawner.builder)
            it.side = spawner.builder.side
            it.radius = spawner.builder.radius
            it.height = spawner.builder.height
            it.step = spawner.builder.step
        }
    }

    private fun syncRay() {
        (obj as Ray).also {
            it.origin = effectOrigin.getLocation(spawner.builder)
            it.maxLength = spawner.builder.maxLength
            it.range = spawner.builder.range
            it.step = spawner.builder.step
            val vector = spawner.builder.vector ?: return
            val direction = taboolib.common.util.Vector(vector.x(), vector.y(), vector.z())
            it.direction = direction
        }
    }

    private fun syncSphere() {
        (obj as Sphere).also {
            it.origin = effectOrigin.getLocation(spawner.builder)
            it.radius = spawner.builder.radius
            it.sample = spawner.builder.sample
        }
    }

    private fun syncStar() {
        (obj as Star).also {
            it.origin = effectOrigin.getLocation(spawner.builder)
        }
    }

    override fun toString(): String {
        return "OrryxParticleObj(effectOrigin=$effectOrigin ,obj=$obj, spawner=$spawner)"
    }

}