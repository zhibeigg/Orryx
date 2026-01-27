package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.IAABB
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

/**
 * 局部坐标系 AABB 接口。
 *
 * @param T 目标位置类型
 * @property localCenter 局部坐标系中心点
 * @property localMin 局部坐标系最小点
 * @property localMax 局部坐标系最大点
 */
interface ILocalAABB<T: ITargetLocation<*>> : IAABB<T>, ILocalCollider<T> {

    var localCenter: Vector3d

    val localMin: Vector3d

    val localMax: Vector3d
}
