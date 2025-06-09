package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.IOBB
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * 局部坐标系OBB碰撞箱
 * */
interface ILocalOBB<T: ITargetLocation<*>> : IOBB<T>, ILocalCollider<T> {

    /**
     * 局部坐标系中心点
     *
     * 设置局部坐标系中心点
     */
    var localCenter: Vector3d

    /**
     * 局部坐标系旋转
     *
     * 设置局部坐标系旋转
     */
    var localRotation: Quaterniond
}
