package org.gitee.orryx.core.selector.geometry

import org.bukkit.entity.LivingEntity
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.getEntityAABB
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import org.joml.RayAabIntersection
import taboolib.common.platform.function.adaptLocation
import taboolib.common5.cfloat
import taboolib.module.effect.createSphere
import taboolib.module.kether.ScriptContext

object Range: ISelectorGeometry {

    override val keys = arrayOf("range")

    override val wiki: Selector
        get() = Selector.new("球形范围", keys, SelectorType.GEOMETRY)
            .addExample("@range 10")
            .addParm(Type.DOUBLE, "半径", "10.0")
            .description("球形范围内的所有实体")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()

        val r = parameter.read<Double>(0, 10.0)
        val ray = RayAabIntersection()

        val entities = origin.world.livingEntities
        return entities.mapNotNull {
            if (it == origin.getSource()) return@mapNotNull it.toTarget()
            if (it is LivingEntity) {
                val dir = it.eyeLocation.toVector().subtract(origin.eyeLocation.toVector()).normalize().multiply(r)
                ray.set(origin.eyeLocation.x.cfloat, origin.eyeLocation.y.cfloat, origin.eyeLocation.z.cfloat, dir.x.cfloat, dir.y.cfloat, dir.z.cfloat)
                val aabb = getEntityAABB(it)
                if (ray.test(aabb.minX.cfloat, aabb.minY.cfloat, aabb.minZ.cfloat, aabb.maxX.cfloat, aabb.maxY.cfloat, aabb.maxZ.cfloat)) {
                    it.toTarget()
                } else null
            } else null
        }
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<taboolib.common.util.Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val r = parameter.read<Double>(0, 10.0)

        return createSphere(adaptLocation(origin.eyeLocation), radius = r).calculateLocations().map { it }
    }
}