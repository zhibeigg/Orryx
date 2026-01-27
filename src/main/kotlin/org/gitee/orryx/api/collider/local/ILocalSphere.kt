package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.ISphere
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

/**
 * 局部坐标系球体碰撞箱接口。
 *
 * @param T 目标位置类型
 * @property localCenter 局部坐标系中心点
 */
interface ILocalSphere<T: ITargetLocation<*>> : ISphere<T>, ILocalCollider<T> {

    var localCenter: Vector3d
}
