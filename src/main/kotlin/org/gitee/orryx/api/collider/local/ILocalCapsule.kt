package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.ICapsule
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

/** 局部坐标系胶囊体碰撞箱 */
interface ILocalCapsule<T: ITargetLocation<*>> : ICapsule<T>, ILocalCollider<T> {

    /**
     * 设置局部坐标系中心点
     * @return 局部坐标系中心点
     */
    var localCenter: Vector3d

    /**
     * 设置局部坐标系旋转
     * @return 局部坐标系旋转
     * */
    var localRotation: Quaterniond
}
