package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.IComposite
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

interface ILocalComposite<T: ITargetLocation<*>, C : ILocalCollider<T>> : IComposite<T, C>, ILocalCollider<T> {

    /**
     * 设置局部坐标系坐标
     * @return  局部坐标系坐标
     */
    var localPosition: Vector3d

    /**
     * 设置局部坐标系旋转
     * @return 局部坐标系旋转
     */
    var localRotation: Quaterniond

    /**
     * @return 坐标
     */
    val position: Vector3d

    /**
     * @return 旋转
     */
    val rotation: Quaterniond

    /**
     * @return 坐标转换器
     */
    val converter: ICoordinateConverter
}