package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.IComposite
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * 局部坐标系复合碰撞箱接口。
 *
 * @param T 目标位置类型
 * @param C 子碰撞箱类型
 * @property localPosition 局部坐标系坐标
 * @property localRotation 局部坐标系旋转
 * @property position 全局坐标
 * @property rotation 全局旋转
 * @property converter 坐标转换器
 */
interface ILocalComposite<T: ITargetLocation<*>, C : ILocalCollider<T>> : IComposite<T, C>, ILocalCollider<T> {

    var localPosition: Vector3d

    var localRotation: Quaterniond

    val position: Vector3d

    val rotation: Quaterniond

    val converter: ICoordinateConverter
}
