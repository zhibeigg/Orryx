package org.gitee.orryx

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.utils.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.console
import taboolib.common.platform.function.disablePlugin
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.module.chat.colored

object OrryxPlugin : Plugin() {

    private val logo = arrayOf(
        "&e┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        "&e┃&a _____                                                  ",
        "&e┃&a/\\  __`\\                                              ",
        "&e┃&a\\ \\ \\/\\ \\  _ __   _ __   __  __   __  _            ",
        "&e┃&a \\ \\ \\ \\ \\/\\`'__\\/\\`'__\\/\\ \\/\\ \\ /\\ \\/'\\",
        "&e┃&a  \\ \\ \\_\\ \\ \\ \\/ \\ \\ \\/ \\ \\ \\_\\ \\\\/>  </",
        "&e┃&a   \\ \\_____\\ \\_\\  \\ \\_\\  \\/`____ \\/\\_/\\_\\  ",
        "&e┃&a    \\/_____/\\/_/   \\/_/   `/___/> \\//\\/_/          ",
        "&e┃&a                             /\\___/                    ",
        "&e┃&a                             \\/__/                     ",
        "&e┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    )

    init {
        registerLifeCycleTask(LifeCycle.INIT) {
            try {
                Orryx.register(OrryxAPI())
            } catch (ex: Throwable) {
                ex.printStackTrace()
                disablePlugin()
            }
        }
    }

    override fun onEnable() {
        consoleMessage(*logo)
    }

    override fun onDisable() {
        consoleMessage("&eOrryx &a卸载")
    }
}