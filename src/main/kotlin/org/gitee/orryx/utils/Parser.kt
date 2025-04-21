package org.gitee.orryx.utils

import org.gitee.orryx.core.kether.actions.effect.EffectType
import org.gitee.orryx.core.parser.StringParser
import taboolib.common.platform.function.warning
import taboolib.common5.*
import taboolib.library.xseries.XMaterial
import taboolib.library.xseries.XParticle

internal inline fun <reified T> StringParser.Entry.read(index: Int, def: T): T {
    val value = body.getOrNull(index) ?: return def
    return when (T::class) {
        String::class -> value
        Int::class -> value.cint
        Long::class -> value.clong
        Boolean::class -> value.cbool
        Double::class -> value.cdouble
        Float::class -> value.cfloat

        XParticle::class -> try {
            XParticle.of(value.uppercase()).get()
        } catch (_: Throwable) {
            warning("not found ProxyParticle")
            XParticle.DUST
        }

        EffectType::class -> try {
            EffectType.valueOf(value.uppercase())
        } catch (_: Throwable) {
            warning("not found EffectType")
            EffectType.ARC
        }

        XMaterial::class -> try {
            XMaterial.matchXMaterial(value.uppercase()).get()
        } catch (_: Throwable) {
            warning("not found Material")
            XMaterial.STONE
        }

        else -> value
    } as T
}