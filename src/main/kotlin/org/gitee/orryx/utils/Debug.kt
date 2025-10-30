package org.gitee.orryx.utils

import org.gitee.nodens.util.debug
import org.gitee.orryx.api.Orryx
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.console
import taboolib.common.platform.service.PlatformIO
import taboolib.module.chat.colored

val debug: Boolean by ConfigLazy(Orryx.config) { Orryx.config.getBoolean("Debug") }

fun debug(vararg message: Any?) {
    if (debug) consoleMessage(*message.map { it.toString() }.toTypedArray())
}

fun consoleMessage(vararg message: String) {
    message.forEach {
        console().sendMessage("[Orryx] $it".colored())
    }
}