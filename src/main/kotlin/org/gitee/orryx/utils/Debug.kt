package org.gitee.orryx.utils

import org.gitee.orryx.api.Orryx
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.service.PlatformIO
import taboolib.module.chat.colored

val debug: Boolean by ConfigLazy(Orryx.config) { Orryx.config.getBoolean("Debug") }

fun debug(vararg message: Any?) {
    if (debug) PlatformFactory.getService<PlatformIO>().info(*message.map { "&6[debug] $it".colored() }.toTypedArray())
}