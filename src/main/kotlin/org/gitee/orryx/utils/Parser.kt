package org.gitee.orryx.utils

import org.bukkit.Material
import org.gitee.orryx.core.kether.actions.effect.EffectType
import org.gitee.orryx.core.parser.StringParser
import taboolib.common.platform.ProxyParticle
import taboolib.common.platform.function.warning
import taboolib.common5.*

internal inline fun <reified T> StringParser.Entry.read(index: Int, def: T): T {
    val value = body.getOrNull(index) ?: return def
    return when (T::class) {
        String::class -> value
        Int::class -> value.cint
        Long::class -> value.clong
        Boolean::class -> value.cbool
        Double::class -> value.cdouble
        Float::class -> value.cfloat
        ProxyParticle::class -> try {
            ProxyParticle.valueOf(value.uppercase())
        } catch (e: Exception) {
            warning("not found ProxyParticle")
            ProxyParticle.valueOf("DUST")
        }
        EffectType::class -> try {
            EffectType.valueOf(value.uppercase())
        } catch (e: Exception) {
            warning("not found EffectType")
            EffectType.valueOf("ARC")
        }
        Material::class -> try {
            Material.valueOf(value.uppercase())
        } catch (e: Exception) {
            warning("not found Material")
            Material.valueOf("STONE")
        }
        else -> value
    } as T
}