package org.gitee.orryx.core.kether.parameter

import org.gitee.orryx.core.targets.ITargetLocation

interface IParameter {

    var origin: ITargetLocation<*>?

    fun getVariable(key: String, lazy: Boolean): Any?

}