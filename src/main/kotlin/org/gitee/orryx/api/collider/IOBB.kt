package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * 有向包围盒 OBB 接口。
 *
 * @param T 目标位置类型
 * @property halfExtents 轴半长
 * @property center 中心点
 * @property rotation 旋转
 * @property vertices 顶点
 * @property axes 轴向
 */
interface IOBB<T: ITargetLocation<*>> : ICollider<T> {

    var halfExtents: Vector3d

    var center: Vector3d

    var rotation: Quaterniond

    val vertices: Array<Vector3d>

    val axes: Array<Vector3d>

    override val type: ColliderType
        get() = ColliderType.OBB
}
