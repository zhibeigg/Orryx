package org.gitee.orryx.core.kether.actions.effect

import org.gitee.orryx.core.targets.ITargetLocation
import taboolib.common.platform.function.adaptLocation
import taboolib.common.util.Location

class EffectOrigin(val bindTarget: ITargetLocation<*>) {

    fun getLocation(builder: EffectBuilder): Location {
        return adaptLocation(bindTarget.location).add(
            builder.translate.x(),
            builder.translate.y(),
            builder.translate.z()
        )
    }

}