package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.IAABB
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

/** 局部坐标系包围盒 */
interface ILocalAABB<T: ITargetLocation<*>> : IAABB<T>, ILocalCollider<T> {

    /**
     * 设置局部坐标系中心点
     * @return 局部坐标系中心点
     */
    var localCenter: Vector3d

    /**
     * @return 局部坐标系最小点
     */
    val localMin: Vector3d

    /**
     * @return 局部坐标系最大点
     */
    val localMax: Vector3d
}
