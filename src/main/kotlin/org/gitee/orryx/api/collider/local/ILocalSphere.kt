package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.ISphere
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

interface ILocalSphere<T: ITargetLocation<*>> : ISphere<T>, ILocalCollider<T> {

    /**
     * 设置局部坐标系中心
     * @return 局部坐标系中心
     */
    var localCenter: Vector3d
}
