package org.gitee.orryx.core.targets

import org.gitee.orryx.api.adapters.AbstractEntity

interface ITargetEntity<T>: ITarget<T> {

    val entity: AbstractEntity

}