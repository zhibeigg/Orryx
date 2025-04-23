package org.gitee.orryx

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.OrryxAPI
import taboolib.common.LifeCycle
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.disablePlugin
import taboolib.common.platform.function.info
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.module.chat.colored

object OrryxPlugin : Plugin() {

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
        info("&e┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━".colored())
        info("&e┃&a _____                                                  ".colored())
        info("&e┃&a/\\  __`\\                                              ".colored())
        info("&e┃&a\\ \\ \\/\\ \\  _ __   _ __   __  __   __  _            ".colored())
        info("&e┃&a \\ \\ \\ \\ \\/\\`'__\\/\\`'__\\/\\ \\/\\ \\ /\\ \\/'\\".colored())
        info("&e┃&a  \\ \\ \\_\\ \\ \\ \\/ \\ \\ \\/ \\ \\ \\_\\ \\\\/>  </".colored())
        info("&e┃&a   \\ \\_____\\ \\_\\  \\ \\_\\  \\/`____ \\/\\_/\\_\\  ".colored())
        info("&e┃&a    \\/_____/\\/_/   \\/_/   `/___/> \\//\\/_/          ".colored())
        info("&e┃&a                             /\\___/                    ".colored())
        info("&e┃&a                             \\/__/                     ".colored())
        info("&e┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━".colored())
    }

    override fun onDisable() {
        info("&eOrryx &a卸载".colored())
    }
}