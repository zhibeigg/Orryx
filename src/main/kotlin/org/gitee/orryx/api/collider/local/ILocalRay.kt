package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.IRay
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

/**
 * 局部坐标系射线碰撞箱接口。
 *
 * @param T 目标位置类型
 * @property localOrigin 局部坐标系起点
 * @property localDirection 局部坐标系方向
 */
interface ILocalRay<T: ITargetLocation<*>> : IRay<T>, ILocalCollider<T> {

    var localOrigin: Vector3d

    var localDirection: Vector3d
}
