package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import org.gitee.orryx.utils.raytrace.FluidHandling
import taboolib.module.kether.KetherParser
import taboolib.module.kether.script
import java.util.concurrent.CompletableFuture

object RayTraceActions {

    @KetherParser(["ray"], namespace = ORRYX_NAMESPACE)
    private fun rayTrace() = combinationParser(
        Action.new("RayTrace光线追踪", "追踪hitBlock", "ray", false)
            .description("根据光线追踪击中的Block位置(此语句运行前请设置原点，默认以玩家为原点)")
            .addEntry("射线向量", Type.VECTOR)
            .addEntry("碰撞范围(1.12.2及以下无效)", Type.DOUBLE)
            .addEntry("流体处理{NONE[忽略流体], SOURCE_ONLY[仅与源流体块碰撞], ALWAYS[与所有流体碰撞(低于1.13无效)]}", Type.STRING)
            .addEntry("是否比对轴对称包围盒", Type.BOOLEAN)
            .addEntry("即使光线未命中任何可碰撞方块，也会返回光线路径中最后的落点", Type.BOOLEAN)
            .result("击中的位置", Type.VECTOR)
    ) {
        it.group(
            vector(),
            double(),
            text(),
            bool(),
            bool()
        ).apply(it) { direction, maxDistance, fluidHandling, checkAxisAlignedBB, returnClosestPos ->
            future {
                val location = script().getParameter().origin?.eyeLocation
                CompletableFuture.completedFuture(location?.world?.rayTraceBlocks(
                    location.joml(),
                    direction,
                    maxDistance,
                    FluidHandling.valueOf(fluidHandling.uppercase()),
                    checkAxisAlignedBB,
                    returnClosestPos
                )?.hitPosition?.abstract())
            }
        }
    }

}