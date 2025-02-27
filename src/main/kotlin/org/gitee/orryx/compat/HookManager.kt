package org.gitee.orryx.compat

import org.bukkit.Bukkit
import org.gitee.orryx.utils.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.common.platform.function.info
import taboolib.module.chat.colored
import taboolib.module.lang.sendLang

object HookManager {

    @Awake(LifeCycle.ENABLE)
    private fun load() {
        info("&e┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━".colored())
        AdyeshachPlugin.load()
        DragonCorePlugin.load()
        DragonArmourersPlugin.load()
        GermPluginPlugin.load()
        MythicMobsPlugin.load()
        RedisChannelPlugin.load()
        OriginAttributePlugin.load()
        AttributePlusPlugin.load()
        PacketEventsPlugin.load()
        ProtocolLibPlugin.load()
    }

    class Plugin(val name: String, val extensionFunction: () -> Unit = {}) {

        val isEnabled
            get() = Bukkit.getPluginManager().isPluginEnabled(name)

        fun load() {
            if (isEnabled) {
                extensionFunction()
                console().sendLang("hook-true", name)
            } else {
                console().sendLang("hook-false", name)
            }
        }

    }

}