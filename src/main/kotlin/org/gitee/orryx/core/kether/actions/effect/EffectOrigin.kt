package org.gitee.orryx.core.kether.actions.effect

import org.bukkit.entity.Entity
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.core.targets.ITargetLocation
import taboolib.common.platform.function.adaptLocation
import taboolib.common.util.Location

class EffectOrigin(val bindTarget: ITargetLocation<*>) {

    fun isValid(): Boolean {
        return when (val source = bindTarget.getSource()) {
            is Entity -> source.isValid && !source.isDead
            is IEntity -> source.isValid
            else -> true
        }
    }

    fun getLocation(builder: EffectBuilder): Location {
        return adaptLocation(bindTarget.location).add(
            builder.translate.x(),
            builder.translate.y(),
            builder.translate.z()
        )
    }

}