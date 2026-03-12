package org.gitee.orryx.core.selector.geometry

import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import taboolib.common.platform.function.adaptLocation
import taboolib.common.util.Location
import taboolib.module.effect.createLine
import taboolib.module.kether.ScriptContext

object LookAt: ISelectorGeometry {

    override val keys = arrayOf("lookat", "look")

    override val wiki: Selector
        get() = Selector.new("视线目标", keys, SelectorType.GEOMETRY)
            .addExample("@lookat 32 5")
            .addParm(Type.DOUBLE, "最大距离", "32.0")
            .addParm(Type.DOUBLE, "角度容差", "5.0")
            .description("选取玩家准星正对的实体（视线方向夹角最小的实体）")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()

        val maxDistance = parameter.read<Double>(0, 32.0)
        val tolerance = parameter.read<Double>(1, 5.0)

        val eyeLoc = origin.eyeLocation
        val direction = eyeLoc.direction.clone().normalize()
        val toleranceRad = Math.toRadians(tolerance)

        val entities = origin.world.getNearbyEntities(eyeLoc, maxDistance, maxDistance, maxDistance)

        // 找夹角最小的实体
        var bestTarget: org.bukkit.entity.Entity? = null
        var bestAngle = toleranceRad

        for (entity in entities) {
            if (entity.location == eyeLoc) continue
            val toEntity = entity.location.clone().add(0.0, entity.height / 2, 0.0)
                .toVector().subtract(eyeLoc.toVector())
            val dist = toEntity.length()
            if (dist > maxDistance || dist < 0.1) continue

            val angle = toEntity.normalize().angle(direction).toDouble()
            if (angle < bestAngle) {
                bestAngle = angle
                bestTarget = entity
            }
        }

        return bestTarget?.let { listOf(it.toTarget()) } ?: emptyList()
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val maxDistance = parameter.read<Double>(0, 32.0)

        val eyeLoc = origin.eyeLocation
        val endPoint = eyeLoc.clone().add(eyeLoc.direction.clone().normalize().multiply(maxDistance))

        return createLine(adaptLocation(eyeLoc), adaptLocation(endPoint)).calculateLocations()
    }
}
