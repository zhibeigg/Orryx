package org.gitee.orryx.core.selector.geometry

import org.bukkit.util.Vector
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ensureSync
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import taboolib.common.platform.function.adaptLocation
import taboolib.module.effect.createLine
import taboolib.module.kether.ScriptContext
import kotlin.math.abs
import kotlin.math.sqrt

object OrientedBoundingBox: ISelectorGeometry {

    override val keys = arrayOf("obb")

    override val wiki: Selector
        get() = Selector.new("有向包围盒", keys, SelectorType.GEOMETRY)
            .addExample("@obb 2 2 2 2 1 true")
            .addParm(Type.DOUBLE, "长度", "0.0")
            .addParm(Type.DOUBLE, "宽度", "0.0")
            .addParm(Type.DOUBLE, "高度", "0.0")
            .addParm(Type.DOUBLE, "前方偏移", "0.0")
            .addParm(Type.DOUBLE, "上方偏移", "0.0")
            .addParm(Type.BOOLEAN, "是否随俯仰角改变", "false")
            .description("选中视角方向的给定长宽高碰撞箱接触的实体")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()
        val location = origin.eyeLocation
        val long = parameter.read<Double>(0, 0.0)
        val wide = parameter.read<Double>(1, 0.0)
        val high = parameter.read<Double>(2, 0.0)
        val forward = parameter.read<Double>(3, 0.0)
        val offsetY = parameter.read<Double>(4, 0.0)
        val pitch = parameter.read<Boolean>(5, false)

        if (!pitch) { location.pitch = 0f }

        // 方向向量生成
        val vectorZ1 = location.direction.clone().setY(0).normalize().crossProduct(Vector(0, 1, 0)).normalize()
        val vectorX1 = location.direction.clone().normalize()
        val vectorY1 = vectorZ1.clone().crossProduct(vectorX1)

        // OBB参数计算
        val obbCenterLocation = location.clone()
            .add(vectorX1.clone().multiply(forward))
            .add(vectorY1.clone().multiply(offsetY))
        val obbCenter = obbCenterLocation.toVector()
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

        val entities = ensureSync { origin.world.livingEntities }.join()

        return entities.filter { entity ->
            val entityLoc = entity.location
            val aabbCenter = Vector(entityLoc.x, entityLoc.y + entity.height / 2, entityLoc.z)
            val aabbExtents = Triple(entity.width / 2, entity.height / 2, entity.width / 2)
            val t = aabbCenter.subtract(obbCenter)

            // 分离轴检测（轴列表与计算逻辑不变）
            val axisList = mutableListOf(
                vectorX1, vectorY1, vectorZ1,
                Vector(1.0, 0.0, 0.0), Vector(0.0, 1.0, 0.0), Vector(0.0, 0.0, 1.0)
            ).apply {
                listOf(vectorX1, vectorY1, vectorZ1).forEach { a ->
                    listOf(Vector(1.0, 0.0, 0.0), Vector(0.0, 1.0, 0.0), Vector(0.0, 0.0, 1.0)).forEach { b ->
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
        val long = parameter.read<Double>(0, 0.0)
        val wide = parameter.read<Double>(1, 0.0)
        val high = parameter.read<Double>(2, 0.0)
        val forward = parameter.read<Double>(3, 0.0)
        val offsetY = parameter.read<Double>(4, 0.0)
        val pitch = parameter.read<Boolean>(5, false)

        if (!pitch) { location.pitch = 0f }

        val list = mutableListOf<taboolib.common.util.Location>()

        val vectorZ1 = location.direction.clone().setY(0).normalize().crossProduct(Vector(0, 1, 0)).normalize()
        val vectorX1 = location.direction.clone().normalize()
        val vectorY1 = vectorZ1.clone().crossProduct(vectorX1)

        val upFLeft = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward + long / 2)).add(vectorY1.clone().multiply(offsetY + high / 2)).add(vectorZ1.clone().multiply(- wide / 2)))
        val upFRight = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward + long / 2)).add(vectorY1.clone().multiply(offsetY + high / 2)).add(vectorZ1.clone().multiply(wide / 2)))
        val upBLeft = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward - long / 2)).add(vectorY1.clone().multiply(offsetY + high / 2)).add(vectorZ1.clone().multiply(- wide / 2)))
        val upBRight = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward - long / 2)).add(vectorY1.clone().multiply(offsetY + high / 2)).add(vectorZ1.clone().multiply(wide / 2)))

        val downFLeft = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward + long / 2)).add(vectorY1.clone().multiply(offsetY - high / 2)).add(vectorZ1.clone().multiply(- wide / 2)))
        val downFRight = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward + long / 2)).add(vectorY1.clone().multiply(offsetY - high / 2)).add(vectorZ1.clone().multiply(wide / 2)))
        val downBLeft = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward - long / 2)).add(vectorY1.clone().multiply(offsetY - high / 2)).add(vectorZ1.clone().multiply(- wide / 2)))
        val downBRight = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward - long / 2)).add(vectorY1.clone().multiply(offsetY - high / 2)).add(vectorZ1.clone().multiply(wide / 2)))

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
