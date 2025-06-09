package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.IRay
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

interface ILocalRay<T: ITargetLocation<*>> : IRay<T>, ILocalCollider<T> {

    /**
     * 设置局部坐标系起点
     * @return 局部坐标系起点
     */
    var localOrigin: Vector3d

    /**
     * 设置局部坐标系方向
     * @return 局部坐标系方向
     */
    var localDirection: Vector3d
}
