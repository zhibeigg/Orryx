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

object Cylinder: ISelectorGeometry {

    override val keys = arrayOf("cylinder", "cyl")

    override val wiki: Selector
        get() = Selector.new("圆柱体范围", keys, SelectorType.GEOMETRY)
            .addExample("@cylinder 5 10")
            .addParm(Type.DOUBLE, "半径", "5.0")
            .addParm(Type.DOUBLE, "高度", "10.0")
            .addParm(Type.DOUBLE, "前方偏移", "0.0")
            .addParm(Type.DOUBLE, "y轴偏移", "0.0")
            .addParm(Type.BOOLEAN, "跟随pitch", "false")
            .description("以玩家朝向为轴心的圆柱体范围选取")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()

        val radius = parameter.read<Double>(0, 5.0)
        val height = parameter.read<Double>(1, 10.0)
        val forward = parameter.read<Double>(2, 0.0)
        val offsetY = parameter.read<Double>(3, 0.0)
        val followPitch = parameter.read<Boolean>(4, false)

        val eyeLoc = origin.eyeLocation.clone()
        if (!followPitch) { eyeLoc.pitch = 0f }
        eyeLoc.y += offsetY

        val axis = eyeLoc.direction.clone().normalize()
        // 将起点沿轴线偏移
        val startPoint = eyeLoc.clone().add(axis.clone().multiply(forward))

        val searchRadius = maxOf(radius, height) + forward
        val entities = origin.world.getNearbyEntities(origin.eyeLocation, searchRadius, searchRadius, searchRadius)

        return entities.mapNotNull { entity ->
            val entityCenter = entity.location.clone().add(0.0, entity.height / 2, 0.0)
            val toEntity = entityCenter.toVector().subtract(startPoint.toVector())

            // 实体在轴线上的投影距离
            val projDist = toEntity.dot(axis)
            if (projDist < 0 || projDist > height) return@mapNotNull null

            // 实体到轴线的垂直距离
            val projPoint = axis.clone().multiply(projDist)
            val perpDist = toEntity.clone().subtract(projPoint).length()

            // 考虑实体宽度
            val entityOffset = entity.width / 2

            if (perpDist <= radius + entityOffset) {
                entity.toTarget()
            } else {
                null
            }
        }
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val radius = parameter.read<Double>(0, 5.0)
        val height = parameter.read<Double>(1, 10.0)
        val forward = parameter.read<Double>(2, 0.0)
        val offsetY = parameter.read<Double>(3, 0.0)
        val followPitch = parameter.read<Boolean>(4, false)

        val eyeLoc = origin.eyeLocation.clone()
        if (!followPitch) { eyeLoc.pitch = 0f }
        eyeLoc.y += offsetY

        val dir = eyeLoc.direction.clone().normalize()
        val locations = mutableListOf<Location>()

        // 构建垂直于轴线的正交基
        val right = dir.clone().setY(0).normalize().crossProduct(Vector(0, 1, 0)).normalize()
        val up = right.clone().crossProduct(dir).normalize()

        val topCenter = eyeLoc.clone().add(dir.clone().multiply(forward))
        val bottomCenter = eyeLoc.clone().add(dir.clone().multiply(forward + height))

        // 绘制两端圆
        if (radius > 0) {
            locations += createCircle(adaptLocation(topCenter), radius, 5.0, 0).calculateLocations()
            locations += createCircle(adaptLocation(bottomCenter), radius, 5.0, 0).calculateLocations()
        }

        // 4条母线
        val lineCount = 4
        for (i in 0 until lineCount) {
            val angle = Math.toRadians(360.0 / lineCount * i)
            val offsetDir = right.clone().multiply(cos(angle)).add(up.clone().multiply(sin(angle)))

            val topPoint = topCenter.clone().add(offsetDir.clone().multiply(radius))
            val bottomPoint = bottomCenter.clone().add(offsetDir.clone().multiply(radius))

            locations += createLine(adaptLocation(topPoint), adaptLocation(bottomPoint)).calculateLocations()
        }

        return locations
    }
}
