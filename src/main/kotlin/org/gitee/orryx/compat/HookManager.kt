package org.gitee.orryx.compat

import org.bukkit.Bukkit
import org.gitee.orryx.utils.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.module.lang.sendLang

object HookManager {

    @Awake(LifeCycle.ENABLE)
    private fun load() {
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

    class Plugin(val name: String) {

        val isEnabled
            get() = Bukkit.getPluginManager().isPluginEnabled(name)

        fun load() {
            console().sendLang(if (isEnabled) "hook-true" else "hook-false", name)
        }

    }

}