package org.gitee.orryx.core.kether.actions.effect

import org.bukkit.util.Vector
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.utils.joml
import org.gitee.orryx.utils.taboo
import org.gitee.orryx.utils.toLocation
import org.joml.Matrix4d
import org.joml.Vector3d
import taboolib.common.platform.function.adaptLocation
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.common.util.Location
import taboolib.common5.cdouble
import taboolib.module.effect.ParticleObj
import taboolib.module.effect.shape.*

class OrryxParticleObj(var bindTarget: ITargetLocation<*>, val obj: ParticleObj, val spawner: EffectSpawner) {

    private var task: PlatformExecutor.PlatformTask? = null

    fun start() {
        obj.spawner = spawner
        spawner.builder.matrix?.taboo()?.let { obj.addMatrix(it) }
        if (spawner.duration <= 1L) {
            obj.show()
        } else {
            obj.alwaysPlayAsync()
            createTask()
        }
    }

    private fun createTask() {
        var delay = 0L
        task = submit(period = 1) {
            if (delay >= spawner.duration) {
                task = null
                cancel()
                obj.turnOffTask()
            }
            if (delay % spawner.tick == 0L) {
                spawner.func()
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
                spawner.builder.matrix?.taboo()?.let { obj.setMatrix(it) }
            }
            delay ++
        }
    }

    fun stop() {
        task?.cancel()
        task = null
        obj.turnOffTask()
    }

    private fun syncArc() {
        (obj as Arc).also {
            it.origin = adaptLocation(bindTarget.location)
            it.startAngle = spawner.builder.startAngle
            it.angle = spawner.builder.angle
            it.radius = spawner.builder.radius
            it.step = spawner.builder.step
        }
    }

    private fun syncAstroid() {
        (obj as Astroid).also {
            it.origin = adaptLocation(bindTarget.location)
            it.radius = spawner.builder.radius
            it.step = spawner.builder.step
        }
    }

    private fun syncCube() {
        val o = bindTarget.location.toVector().joml()
        val width = spawner.builder.width
        val height = spawner.builder.height
        val length = spawner.builder.length
        val min = o.add(-0.5, -0.5, -0.5, Vector3d()).toLocation()
        val max = o.add(0.5, 0.5, 0.5, Vector3d())
        val z = bindTarget.location.direction.clone().setY(0).normalize().crossProduct(Vector(0, 1, 0)).joml()
        val matrix = Matrix4d().scale(width, height, length).rotateY(bindTarget.location.yaw.cdouble).rotate(bindTarget.location.pitch.cdouble, z)
        (obj as Cube).also {
            it.minLocation =  Location(bindTarget.world.name, min.x, min.y, min.z)
            it.maxLocation = Location(bindTarget.world.name, max.x, max.y, max.z)
            it.step = spawner.builder.step
            it.setMatrix(matrix.taboo())
        }
    }

    private fun syncHeart() {
        (obj as Heart).also {
            it.origin = adaptLocation(bindTarget.location)
            it.xScaleRate = spawner.builder.xScaleRate
            it.yScaleRate = spawner.builder.yScaleRate
            it.step = spawner.builder.step
        }
    }

    private fun syncLine() {
        val start = adaptLocation(bindTarget.location)
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
            it.origin = adaptLocation(bindTarget.location)
        }
    }

    private fun syncNStar() {
        (obj as NStar).also {
            it.origin = adaptLocation(bindTarget.location)
            it.corner = spawner.builder.corner
            it.radius = spawner.builder.radius
            it.step = spawner.builder.step
        }
    }

    private fun syncOctagonalStar() {
        (obj as OctagonalStar).also {
            it.origin = adaptLocation(bindTarget.location)
            it.radius = spawner.builder.radius
            it.step = spawner.builder.step
        }
    }

    private fun syncPolygon() {
        (obj as Polygon).also {
            it.origin = adaptLocation(bindTarget.location)
            it.radius = spawner.builder.radius
            it.side = spawner.builder.side
            it.step = spawner.builder.step
        }
    }

    private fun syncPyramid() {
        (obj as Pyramid).also {
            it.origin = adaptLocation(bindTarget.location)
            it.side = spawner.builder.side
            it.radius = spawner.builder.radius
            it.height = spawner.builder.height
            it.step = spawner.builder.step
        }
    }

    private fun syncRay() {
        (obj as Ray).also {
            it.origin = adaptLocation(bindTarget.location)
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
            it.origin = adaptLocation(bindTarget.location)
            it.radius = spawner.builder.radius
            it.sample = spawner.builder.sample
        }
    }

    private fun syncStar() {
        (obj as Star).also {
            it.origin = adaptLocation(bindTarget.location)
        }
    }

    override fun toString(): String {
        return "OrryxParticleObj(bindTarget=$bindTarget ,obj=$obj, spawner=$spawner)"
    }

}