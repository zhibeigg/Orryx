package org.gitee.orryx.core.targets

import org.gitee.orryx.api.adapters.IEntity

/**
 * 具备实体信息的目标对象接口。
 *
 * @param T 目标的源对象类型
 * @property entity 目标实体适配器
 */
interface ITargetEntity<T>: ITarget<T> {

    val entity: IEntity
}
