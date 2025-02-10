package org.gitee.orryx.core.selector.geometry

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.gitee.orryx.api.adapters.IVector
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import org.joml.RayAabIntersection
import org.joml.Vector3d
import taboolib.common5.cfloat
import taboolib.module.kether.ScriptContext

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
        val distance = parameter.read<Double>(1, "0.0")
        val vector = context.get<IVector>(v)?.joml ?: return emptyList()

        return findEntitiesAlongRay(origin.location, vector, distance).map { it.toTarget() }
    }

    override fun showAFrame(context: ScriptContext, parameter: StringParser.Entry): List<Location> {
        val origin = context.getParameter().origin ?: return emptyList()
        val v = parameter.read<String>(0, "a")
        val distance = parameter.read<Double>(1, "0.0")
        val vector = context.get<IVector>(v)?.joml ?: return emptyList()

        val normal = AbstractVector(vector.normalize(0.1, Vector3d())).getBukkit()
        var length = vector.length()
        val start = origin.location.clone()
        val list = mutableListOf<Location>()
        while (length > 0) {
            list += start.add(normal).clone()
            length -= 0.1
        }
        return list
    }

    private fun findEntitiesAlongRay(start: Location, direction: Vector3d, distance: Double): List<Entity> {
        val origin = Vector3d(start.x, start.y, start.z)

        val world = start.world ?: return emptyList()
        val maxDistance = direction.length()
        val nearbyEntities = world.getNearbyEntities(start, maxDistance, maxDistance, maxDistance)

        val entitiesWithDistance = mutableListOf<Entity>()

        val ray = RayAabIntersection(origin.x.cfloat, origin.y.cfloat, origin.z.cfloat, direction.x.cfloat, direction.y.cfloat, direction.z.cfloat)

        for (entity in nearbyEntities) {
            val aabb = getEntityAABB(entity)

            if (ray.test(aabb.minX.cfloat, aabb.minY.cfloat, aabb.minZ.cfloat, aabb.maxX.cfloat, aabb.maxY.cfloat, aabb.maxZ.cfloat)) {
                entitiesWithDistance.add(entity)
            }
        }

        return entitiesWithDistance
    }

    private fun getEntityAABB(entity: Entity): AABB {
        return getManualAABB(entity)
    }

    private fun getManualAABB(entity: Entity): AABB {
        val loc = entity.location
        val (width, height) = entity.width to entity.height
        return AABB(
            loc.x - width / 2, loc.y, loc.z - width / 2,
            loc.x + width / 2, loc.y + height, loc.z + width / 2
        )
    }

    data class AABB(
        val minX: Double,
        val minY: Double,
        val minZ: Double,
        val maxX: Double,
        val maxY: Double,
        val maxZ: Double
    )

}