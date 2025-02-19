package org.gitee.orryx.core.selector.geometry

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.utils.*
import org.joml.RayAabIntersection
import org.joml.Vector3d
import taboolib.common.platform.function.adaptLocation
import taboolib.common5.cfloat
import taboolib.module.kether.ScriptContext
import java.util.stream.Collectors

/**
 * 选中向量穿过的所有实体
 * @rayhit vector distance
 */
object RayHit: ISelectorGeometry {

    override val keys: Array<String>
        get() = arrayOf("rayhit")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()
        val v = parameter.read<String>(0, "a")
        val distance = parameter.read<Double>(1, 0.0)
        val vector = context.vector(v)?.joml ?: return emptyList()

        return findEntitiesAlongRay(origin.eyeLocation, vector, distance).map { it.toTarget() }
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<taboolib.common.util.Location> {
        val origin = context.getParameter().origin ?: return emptyList()
        val v = parameter.read<String>(0, "a")
        val distance = parameter.read<Double>(1, 0.0)
        val vector = context.vector(v)?.joml ?: return emptyList()

        val normal = vector.normalize(0.1, Vector3d()).taboo()
        var length = vector.length()
        val start = adaptLocation(origin.eyeLocation)
        val list = mutableListOf<taboolib.common.util.Location>()
        while (length > 0) {
            list += start.add(normal).clone()
            length -= 0.1
        }
        return list
    }

    private fun findEntitiesAlongRay(origin: Location, direction: Vector3d, distance: Double): List<Entity> {
        val world = origin.world ?: return emptyList()

        val length = direction.length()

        val nearbyEntities = world.getNearbyEntities(origin, length, length, length)

        // 初始化射线
        val ray = RayAabIntersection(
            origin.x.cfloat, origin.y.cfloat, origin.z.cfloat,
            direction.x.cfloat, direction.y.cfloat, direction.z.cfloat
        )

        // 检测相交并记录距离
        val entitiesWithDistance = nearbyEntities.parallelStream().filter { entity ->
            val aabb = getEntityAABB(entity)
            ray.test(aabb.minX.cfloat, aabb.minY.cfloat, aabb.minZ.cfloat, aabb.maxX.cfloat, aabb.maxY.cfloat, aabb.maxZ.cfloat)
        }.collect(Collectors.toList())

        return entitiesWithDistance
    }

}