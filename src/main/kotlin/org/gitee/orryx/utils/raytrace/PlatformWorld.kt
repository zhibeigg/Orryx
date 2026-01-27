package org.gitee.orryx.utils.raytrace

import org.joml.Vector3dc

/**
 * 平台世界光线追踪接口。
 */
interface PlatformWorld {

    /**
     * 执行方块碰撞的光线追踪计算。
     *
     * @param start 原点向量
     * @param direction 射线向量
     * @param maxDistance 碰撞范围（1.12.2 及以下无效）
     * @param fluidHandling 流体处理
     * @param checkAxisAlignedBB 是否比对轴对称包围盒
     * @param returnClosestPos 即使光线未命中任何可碰撞方块，也会返回光线路径中最后的落点
     * @return 光线追踪结果，未命中时返回 null
     */
    fun rayTraceBlocks(
        start: Vector3dc,
        direction: Vector3dc,
        maxDistance: Double,
        fluidHandling: FluidHandling,
        checkAxisAlignedBB: Boolean,
        returnClosestPos: Boolean
    ): RayTraceResult?
}
