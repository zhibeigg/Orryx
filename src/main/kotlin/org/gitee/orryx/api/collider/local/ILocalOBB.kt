package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.IOBB
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * 局部坐标系 OBB 碰撞箱接口。
 *
 * @param T 目标位置类型
 * @property localCenter 局部坐标系中心点
 * @property localRotation 局部坐标系旋转
 */
interface ILocalOBB<T: ITargetLocation<*>> : IOBB<T>, ILocalCollider<T> {

    var localCenter: Vector3d

    var localRotation: Quaterniond
}
