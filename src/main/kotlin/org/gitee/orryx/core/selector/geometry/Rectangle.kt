package org.gitee.orryx.core.selector.geometry

import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.selector.stream.Server
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.core.wiki.Selector
import org.gitee.orryx.core.wiki.SelectorType
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import org.joml.Vector3d
import taboolib.module.effect.createCube
import taboolib.module.kether.ScriptContext

object Rectangle: ISelectorGeometry {

    override val keys = arrayOf("rec", "rectangle")

    override val wiki: Selector
        get() = Selector.new("碰撞箱", Server.keys, SelectorType.GEOMETRY)
            .addExample("@rectangle 2 2 2 2 1 true")
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

        val vectorX = location.direction.clone().setY(0).joml().normalize()
        val vectorZ = vectorX.cross(Vector3d(0.0, 1.0, 0.0), Vector3d()).normalize()
        val vectorY = location.direction.clone().joml().cross(vectorZ, Vector3d()).normalize()
        val minVector = AbstractVector(location.toVector()).add(vectorX.mul(-long/2+forward, Vector3d())).add(vectorY.mul(-high/2+offsetY)).add(vectorZ.mul(-wide/2, Vector3d()))
        val maxVector = AbstractVector(location.toVector()).add(vectorX.mul(long/2+forward, Vector3d())).add(vectorY.mul(high/2+offsetY)).add(vectorZ.mul(wide/2, Vector3d()))

        val box = AABB(minVector, maxVector)

        return origin.world.livingEntities.filter {
            it != origin && areAABBsColliding(box, getEntityAABB(it))
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

        val vectorX = location.direction.clone().setY(0).joml().normalize()
        val vectorZ = vectorX.cross(Vector3d(0.0, 1.0, 0.0), Vector3d()).normalize()
        val vectorY = location.direction.clone().joml().cross(vectorZ, Vector3d()).normalize()
        val minVector = AbstractVector(location.toVector()).add(vectorX.mul(-long/2+forward, Vector3d())).add(vectorY.mul(-high/2+offsetY)).add(vectorZ.mul(-wide/2, Vector3d()))
        val maxVector = AbstractVector(location.toVector()).add(vectorX.mul(long/2+forward, Vector3d())).add(vectorY.mul(high/2+offsetY)).add(vectorZ.mul(wide/2, Vector3d()))

        return createCube(taboolib.common.util.Location(location.world?.name, minVector.x(), minVector.y(), minVector.z()), taboolib.common.util.Location(location.world?.name, maxVector.x(), maxVector.y(), maxVector.z())).calculateLocations().map { it }
    }

}