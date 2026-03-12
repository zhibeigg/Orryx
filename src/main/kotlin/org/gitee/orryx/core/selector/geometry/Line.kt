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
import taboolib.module.effect.createLine
import taboolib.module.kether.ScriptContext
import kotlin.math.abs

object Line: ISelectorGeometry {

    override val keys = arrayOf("line")

    override val wiki: Selector
        get() = Selector.new("线段范围", keys, SelectorType.GEOMETRY)
            .addExample("@line 10 1 2")
            .addParm(Type.DOUBLE, "长度", "10.0")
            .addParm(Type.DOUBLE, "宽度", "1.0")
            .addParm(Type.DOUBLE, "高度", "2.0")
            .addParm(Type.BOOLEAN, "跟随pitch", "false")
            .description("沿朝向方向的窄长方体，前方偏移固定为长度/2，适合剑气、直线冲击波类技能")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()
        val location = origin.eyeLocation
        val long = parameter.read<Double>(0, 10.0)
        val wide = parameter.read<Double>(1, 1.0)
        val high = parameter.read<Double>(2, 2.0)
        val pitch = parameter.read<Boolean>(3, false)

        if (!pitch) { location.pitch = 0f }

        // 前方偏移固定为 length/2
        val forward = long / 2

        // 方向向量生成（复用OBB逻辑）
        val vectorZ1 = location.direction.clone().setY(0).normalize().crossProduct(Vector(0, 1, 0)).normalize()
        val vectorX1 = location.direction.clone().normalize()
        val vectorY1 = vectorZ1.clone().crossProduct(vectorX1)

        // OBB参数计算
        val obbCenter = location.clone()
            .add(vectorX1.clone().multiply(forward))
            .toVector()
        val obbExtents = Triple(long, high, wide)

        fun overlapOnAxis(axis: Vector, t: Vector, aabbExtents: Triple<Double, Double, Double>): Boolean {
            val projection = abs(t.dot(axis))
            val rObb = (obbExtents.first / 2) * abs(axis.dot(vectorX1)) +
                    (obbExtents.second / 2) * abs(axis.dot(vectorY1)) +
                    (obbExtents.third / 2) * abs(axis.dot(vectorZ1))
            val rAabb = aabbExtents.first * abs(axis.x) +
                    aabbExtents.second * abs(axis.y) +
                    aabbExtents.third * abs(axis.z)
            return projection <= rObb + rAabb + 1e-6
        }

        val entities = origin.world.livingEntities

        return entities.filter { entity ->
            val entityLoc = entity.location
            val aabbCenter = Vector(entityLoc.x, entityLoc.y + entity.height / 2, entityLoc.z)
            val aabbExtents = Triple(entity.width / 2, entity.height / 2, entity.width / 2)
            val t = aabbCenter.subtract(obbCenter)

            val axisList = mutableListOf(
                vectorX1, vectorY1, vectorZ1,
                Vector(1.0, 0.0, 0.0), Vector(0.0, 1.0, 0.0), Vector(0.0, 0.0, 1.0)
            ).apply {
                arrayOf(vectorX1, vectorY1, vectorZ1).forEach { a ->
                    arrayOf(Vector(1.0, 0.0, 0.0), Vector(0.0, 1.0, 0.0), Vector(0.0, 0.0, 1.0)).forEach { b ->
                        val cross = a.clone().crossProduct(b)
                        if (cross.lengthSquared() > 1e-6) add(cross.normalize())
                    }
                }
            }

            axisList.all { axis -> overlapOnAxis(axis, t, aabbExtents) }
        }.map { it.toTarget() }
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<taboolib.common.util.Location> {
        val origin = context.getParameter().origin ?: return emptyList()
        val location = origin.eyeLocation
        val long = parameter.read<Double>(0, 10.0)
        val wide = parameter.read<Double>(1, 1.0)
        val high = parameter.read<Double>(2, 2.0)
        val pitch = parameter.read<Boolean>(3, false)

        if (!pitch) { location.pitch = 0f }

        val forward = long / 2

        val list = mutableListOf<taboolib.common.util.Location>()

        val vectorZ1 = location.direction.clone().setY(0).normalize().crossProduct(Vector(0, 1, 0)).normalize()
        val vectorX1 = location.direction.clone().normalize()
        val vectorY1 = vectorZ1.clone().crossProduct(vectorX1)

        val upFLeft = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward + long / 2)).add(vectorY1.clone().multiply(high / 2)).add(vectorZ1.clone().multiply(- wide / 2)))
        val upFRight = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward + long / 2)).add(vectorY1.clone().multiply(high / 2)).add(vectorZ1.clone().multiply(wide / 2)))
        val upBLeft = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward - long / 2)).add(vectorY1.clone().multiply(high / 2)).add(vectorZ1.clone().multiply(- wide / 2)))
        val upBRight = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward - long / 2)).add(vectorY1.clone().multiply(high / 2)).add(vectorZ1.clone().multiply(wide / 2)))

        val downFLeft = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward + long / 2)).add(vectorY1.clone().multiply(- high / 2)).add(vectorZ1.clone().multiply(- wide / 2)))
        val downFRight = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward + long / 2)).add(vectorY1.clone().multiply(- high / 2)).add(vectorZ1.clone().multiply(wide / 2)))
        val downBLeft = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward - long / 2)).add(vectorY1.clone().multiply(- high / 2)).add(vectorZ1.clone().multiply(- wide / 2)))
        val downBRight = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward - long / 2)).add(vectorY1.clone().multiply(- high / 2)).add(vectorZ1.clone().multiply(wide / 2)))

        //上顶面
        list += createLine(upFLeft, upFRight).calculateLocations()
        list += createLine(upBLeft, upBRight).calculateLocations()
        list += createLine(upFLeft, upBLeft).calculateLocations()
        list += createLine(upFRight, upBRight).calculateLocations()

        //下面
        list += createLine(downFLeft, downFRight).calculateLocations()
        list += createLine(downBLeft, downBRight).calculateLocations()
        list += createLine(downFLeft, downBLeft).calculateLocations()
        list += createLine(downFRight, downBRight).calculateLocations()

        //四柱
        list += createLine(upFLeft, downFLeft).calculateLocations()
        list += createLine(upFRight, downFRight).calculateLocations()
        list += createLine(upBLeft, downBLeft).calculateLocations()
        list += createLine(upBRight, downBRight).calculateLocations()

        return list
    }
}
