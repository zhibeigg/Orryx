package org.gitee.orryx.core.kether.parameter

import org.gitee.orryx.core.targets.ITarget

interface IParameter {

    var origin: ITarget<*>?

    fun getVariable(key: String, lazy: Boolean): Any?

}