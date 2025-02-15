package org.gitee.orryx.utils

import org.gitee.orryx.core.kether.actions.effect.EffectBuilder
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.script

fun ScriptFrame.effectBuilder(): EffectBuilder? {
    return script().get<EffectBuilder>("@effect")
}