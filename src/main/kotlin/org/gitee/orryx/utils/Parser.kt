package org.gitee.orryx.utils

import org.bukkit.Material
import org.gitee.orryx.core.parser.StringParser
import taboolib.common.platform.function.warning
import taboolib.common5.*

internal inline fun <reified T> StringParser.Entry.read(index: Int, def: T): T {
    val value = body.getOrElse(index) { def }
    return when (T::class) {
        String::class -> value
        Int::class -> value.cint
        Long::class -> value.clong
        Boolean::class -> value.cbool
        Double::class -> value.cdouble
        Float::class -> value.cfloat
        Material::class -> try {
            value as? Material ?: Material.valueOf(value.toString().uppercase())
        } catch (e: Exception) {
            warning("not found Material")
            Material.valueOf("STONE")
        }
        else -> value
    } as T
}