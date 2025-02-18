package org.gitee.orryx.core.selector.geometry

import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.utils.getEntityAABB
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import org.joml.RayAabIntersection
import taboolib.common.platform.function.adaptLocation
import taboolib.common.platform.function.platformLocation
import taboolib.common5.cfloat
import taboolib.module.effect.createSphere
import taboolib.module.kether.ScriptContext

/**
 * 球形范围内
 * @range 10
 */
object Range: ISelectorGeometry {

    override val keys: Array<String>
        get() = arrayOf("range")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()

        val r = parameter.read<Double>(0, 10.0)

        val entities = origin.world.getNearbyEntities(origin.eyeLocation, r, r, r)
        return entities.mapNotNull {
            if (it is LivingEntity) {
                val dir = it.eyeLocation.toVector().subtract(origin.eyeLocation.toVector()).normalize().multiply(r)
                val ray = RayAabIntersection(origin.eyeLocation.x.cfloat, origin.eyeLocation.y.cfloat, origin.eyeLocation.z.cfloat, dir.x.cfloat, dir.y.cfloat, dir.z.cfloat)
                val aabb = getEntityAABB(it)
                if (ray.test(aabb.minX.cfloat, aabb.minY.cfloat, aabb.minZ.cfloat, aabb.maxX.cfloat, aabb.maxY.cfloat, aabb.maxZ.cfloat)) {
                    it.toTarget()
                } else null
            } else null
        }
    }

    override fun showAFrame(context: ScriptContext, parameter: StringParser.Entry): List<Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val r = parameter.read<Double>(0, 10.0)

        return createSphere(adaptLocation(origin.eyeLocation), radius = r).calculateLocations().map { platformLocation(it) }
    }

}