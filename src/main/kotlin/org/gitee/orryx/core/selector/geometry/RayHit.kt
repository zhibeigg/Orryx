package org.gitee.orryx.core.selector.geometry

import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.getEntityAABB
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.segmentIntersectsAabb
import org.gitee.orryx.utils.taboo
import org.gitee.orryx.utils.toTarget
import org.gitee.orryx.utils.vector
import org.joml.Vector3d
import taboolib.common.platform.function.adaptLocation
import taboolib.module.kether.ScriptContext

object RayHit: ISelectorGeometry {

    override val keys = arrayOf("rayhit")

    override val wiki: Selector
        get() = Selector.new("射线实体", keys, SelectorType.GEOMETRY)
            .addExample("@rayhit a 10")
            .addParm(Type.STRING, "存储方向向量的键名", "a")
            .addParm(Type.DOUBLE, "长度", "0.0")
            .description("选中从原点沿向量方向、给定有限长度内穿过的所有实体")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()
        val vectorKey = parameter.read<String>(0, "a")
        val distance = parameter.read<Double>(1, 0.0)
        val sourceDirection = context.vector(vectorKey)?.joml ?: return emptyList()
        if (distance <= 0.0 || sourceDirection.lengthSquared() <= 1e-12) {
            return emptyList()
        }

        val start = origin.eyeLocation.toVector()
        val direction = Vector3d(sourceDirection).normalize(distance)
        val end = start.clone().add(org.bukkit.util.Vector(direction.x, direction.y, direction.z))
        val bounds = GeometryBroadPhase.segmentBounds(start, end)
        val segmentStart = Vector3d(start.x, start.y, start.z)
        val segmentEnd = Vector3d(end.x, end.y, end.z)

        return GeometryBroadPhase.nearbyLivingEntities(origin.world, bounds)
            .filter { entity ->
                val aabb = getEntityAABB(entity)
                segmentIntersectsAabb(
                    segmentStart,
                    segmentEnd,
                    Vector3d(aabb.minX, aabb.minY, aabb.minZ),
                    Vector3d(aabb.maxX, aabb.maxY, aabb.maxZ)
                )
            }
            .map { it.toTarget() }
            .toList()
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<taboolib.common.util.Location> {
        val origin = context.getParameter().origin ?: return emptyList()
        val vectorKey = parameter.read<String>(0, "a")
        val distance = parameter.read<Double>(1, 0.0)
        val vector = context.vector(vectorKey)?.joml ?: return emptyList()
        if (distance <= 0.0 || vector.lengthSquared() <= 1e-12) {
            return emptyList()
        }

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
}
