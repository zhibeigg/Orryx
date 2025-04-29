package org.gitee.orryx.core.kether.parameter

import org.bukkit.entity.Entity
import org.gitee.orryx.core.targets.ITargetLocation

class MythicMobsParameter(val caster: Entity, override var origin: ITargetLocation<*>?): IParameter {

    override fun getVariable(key: String, default: Any): Any {
        return default
    }

    override fun getVariable(key: String, lazy: Boolean): Any? {
        return null
    }
}