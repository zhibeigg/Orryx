package org.gitee.orryx.core.kether.parameter

import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.state.PlayerData
import org.gitee.orryx.utils.toTarget

class StateParameter(val playerData: PlayerData): IParameter {

    override var origin: ITargetLocation<*>? = playerData.player.toTarget()

    override fun getVariable(key: String, lazy: Boolean): Any? {
        return null
    }

    override fun getVariable(key: String, default: Any): Any {
        return default
    }

}