package org.gitee.orryx.core.selector.geometry

import org.bukkit.entity.LivingEntity
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.local.LocalAABB
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.local.LocalSphere
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import org.joml.Vector3d
import taboolib.common.platform.function.adaptLocation
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

        val entities = ensureSync { origin.world.getNearbyEntities(origin.location, r, r, r) }.join()
        val hitbox = LocalSphere<ITargetLocation<*>>(Vector3d(), r, origin.coordinateConverter())
        return entities.mapNotNull {
            if (it == origin.getSource()) return@mapNotNull it.toTarget()
            if (it is LivingEntity) {
                val aabb = LocalAABB<AbstractBukkitEntity>(
                    Vector3d(0.0, it.height / 2, 0.0),
                    Vector3d(it.width / 2, it.height / 2, it.width / 2),
                    it.abstract().coordinateConverter()
                )
                it.toTarget().takeIf { isColliding(hitbox, aabb) }
            } else null
        }
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<taboolib.common.util.Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val r = parameter.read<Double>(0, 10.0)

        return createSphere(adaptLocation(origin.eyeLocation), radius = r).calculateLocations().map { it }
    }
}