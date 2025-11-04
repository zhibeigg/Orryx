package org.gitee.orryx.utils

import org.gitee.orryx.api.Orryx
import taboolib.common.platform.function.console
import taboolib.module.chat.colored

val debug: Boolean by ConfigLazy { Orryx.config.getBoolean("Debug") }

fun debug(vararg message: Any?) {
    if (debug) consoleMessage(*message.map { it.toString() }.toTypedArray())
}

fun consoleMessage(vararg message: String) {
    message.forEach {
        console().sendMessage("[Orryx] $it".colored())
    }
}