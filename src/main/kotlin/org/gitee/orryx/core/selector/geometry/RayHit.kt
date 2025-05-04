package org.gitee.orryx.core.selector.geometry

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import org.joml.RayAabIntersection
import org.joml.Vector3d
import taboolib.common.platform.function.adaptLocation
import taboolib.common5.cfloat
import taboolib.module.kether.ScriptContext

object RayHit: ISelectorGeometry {

    override val keys = arrayOf("rayhit")

    override val wiki: Selector
        get() = Selector.new("射线实体", keys, SelectorType.GEOMETRY)
            .addExample("@rayhit a 10")
            .addParm(Type.STRING, "存储方向向量的键名", "a")
            .addParm(Type.DOUBLE, "长度", "0.0")
            .description("选中向量穿过的所有实体")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()
        val v = parameter.read<String>(0, "a")
        val distance = parameter.read<Double>(1, 0.0)
        val vector = context.vector(v)?.joml?.normalize(distance) ?: return emptyList()

        return findEntitiesAlongRay(origin.eyeLocation, vector).map { it.toTarget() }
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<taboolib.common.util.Location> {
        val origin = context.getParameter().origin ?: return emptyList()
        val v = parameter.read<String>(0, "a")
        val distance = parameter.read<Double>(1, 0.0)
        val vector = context.vector(v)?.joml ?: return emptyList()

        val normal = vector.normalize(0.1, Vector3d()).taboo()
        var length = distance
        val start = adaptLocation(origin.eyeLocation)
        val list = mutableListOf<taboolib.common.util.Location>()
        while (length > 0) {
            list += start.add(normal).clone()
            length -= 0.1
        }
        return list
    }

    private fun findEntitiesAlongRay(origin: Location, direction: Vector3d): List<Entity> {
        val world = origin.world ?: return emptyList()

        val nearbyEntities = ensureSync { world.livingEntities }.join()

        // 初始化射线
        val ray = RayAabIntersection(
            origin.x.cfloat, origin.y.cfloat, origin.z.cfloat,
            direction.x.cfloat, direction.y.cfloat, direction.z.cfloat
        )

        // 检测相交并记录距离
        val entitiesWithDistance = nearbyEntities.filter { entity ->
            val aabb = getEntityAABB(entity)
            ray.test(aabb.minX.cfloat, aabb.minY.cfloat, aabb.minZ.cfloat, aabb.maxX.cfloat, aabb.maxY.cfloat, aabb.maxZ.cfloat)
        }

        return entitiesWithDistance
    }
}