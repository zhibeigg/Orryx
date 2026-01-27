package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.ICollider
import org.gitee.orryx.core.targets.ITargetLocation

/**
 * 本地坐标系碰撞箱接口。
 *
 * @param T 目标位置类型
 */
interface ILocalCollider<T: ITargetLocation<*>> : ICollider<T> {

    /**
     * 更新碰撞箱位置。
     */
    fun update()
}
