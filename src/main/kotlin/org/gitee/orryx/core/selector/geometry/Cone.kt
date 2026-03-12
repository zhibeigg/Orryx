package org.gitee.orryx.core.selector.geometry

import org.bukkit.util.Vector
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
import taboolib.module.effect.createCircle
import taboolib.module.effect.createLine
import taboolib.module.kether.ScriptContext
import kotlin.math.cos
import kotlin.math.sin

object Cone: ISelectorGeometry {

    override val keys = arrayOf("cone")

    override val wiki: Selector
        get() = Selector.new("圆锥体范围", keys, SelectorType.GEOMETRY)
            .addExample("@cone 5 10")
            .addParm(Type.DOUBLE, "底部半径", "5.0")
            .addParm(Type.DOUBLE, "长度", "10.0")
            .addParm(Type.DOUBLE, "偏航角", "0.0")
            .addParm(Type.DOUBLE, "y轴偏移", "0.0")
            .addParm(Type.BOOLEAN, "跟随pitch", "false")
            .description("以玩家眼睛为顶点、朝向为轴的圆锥体范围选取")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()

        val bottomRadius = parameter.read<Double>(0, 5.0)
        val length = parameter.read<Double>(1, 10.0)
        val yawOffset = parameter.read<Double>(2, 0.0)
        val offsetY = parameter.read<Double>(3, 0.0)
        val followPitch = parameter.read<Boolean>(4, false)

        val eyeLoc = origin.eyeLocation.clone()
        if (!followPitch) { eyeLoc.pitch = 0f }
        eyeLoc.yaw += yawOffset.toFloat()
        eyeLoc.y += offsetY

        // 圆锥轴线方向
        val axis = eyeLoc.direction.clone().normalize()

        val searchRadius = maxOf(bottomRadius, length)
        val entities = origin.world.getNearbyEntities(origin.eyeLocation, searchRadius, searchRadius, searchRadius)

        return entities.mapNotNull { entity ->
            val entityCenter = entity.location.clone().add(0.0, entity.height / 2, 0.0)
            val toEntity = entityCenter.toVector().subtract(eyeLoc.toVector())

            // 实体在轴线上的投影距离
            val projDist = toEntity.dot(axis)
            if (projDist < 0 || projDist > length) return@mapNotNull null

            // 实体到轴线的垂直距离
            val projPoint = axis.clone().multiply(projDist)
            val perpDist = toEntity.clone().subtract(projPoint).length()

            // 该距离处的圆锥半径（线性插值，顶点半径为0）
            val t = projDist / length
            val radiusAtDist = bottomRadius * t

            // 考虑实体宽度
            val entityOffset = entity.width / 2

            if (perpDist <= radiusAtDist + entityOffset) {
                entity.toTarget()
            } else {
                null
            }
        }
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val bottomRadius = parameter.read<Double>(0, 5.0)
        val length = parameter.read<Double>(1, 10.0)
        val yawOffset = parameter.read<Double>(2, 0.0)
        val offsetY = parameter.read<Double>(3, 0.0)
        val followPitch = parameter.read<Boolean>(4, false)

        val eyeLoc = origin.eyeLocation.clone()
        if (!followPitch) { eyeLoc.pitch = 0f }
        eyeLoc.yaw += yawOffset.toFloat()
        eyeLoc.y += offsetY

        val dir = eyeLoc.direction.clone().normalize()
        val locations = mutableListOf<Location>()

        // 构建垂直于轴线的正交基
        val right = dir.clone().setY(0).normalize().crossProduct(Vector(0, 1, 0)).normalize()
        val up = right.clone().crossProduct(dir).normalize()

        // 顶点和底部圆心
        val apex = eyeLoc.clone()
        val bottomCenter = eyeLoc.clone().add(dir.clone().multiply(length))

        // 绘制底部圆
        if (bottomRadius > 0) {
            locations += createCircle(adaptLocation(bottomCenter), bottomRadius, 5.0, 0).calculateLocations()
        }

        // 4条母线从顶点到底部圆边缘
        val lineCount = 4
        for (i in 0 until lineCount) {
            val angle = Math.toRadians(360.0 / lineCount * i)
            val offsetDir = right.clone().multiply(cos(angle)).add(up.clone().multiply(sin(angle)))

            val bottomPoint = bottomCenter.clone().add(offsetDir.clone().multiply(bottomRadius))

            locations += createLine(adaptLocation(apex), adaptLocation(bottomPoint)).calculateLocations()
        }

        return locations
    }
}
