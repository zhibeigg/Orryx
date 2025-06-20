package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * 有向包围盒OBB
 * */
interface IOBB<T: ITargetLocation<*>> : ICollider<T> {

    /**
     * 轴半长
     *
     * 设置轴半长
     * */
    var halfExtents: Vector3d

    /**
     * 中心点
     *
     * 设置中心点
     * */
    var center: Vector3d

    /**
     * 设置旋转
     * */
    var rotation: Quaterniond

    /**
     * 顶点
     * */
    val vertices: ArrayList<Vector3d>

    /**
     * 轴向
     * */
    val axes: ArrayList<Vector3d>

    override val type: ColliderType
        get() = ColliderType.OBB
}
