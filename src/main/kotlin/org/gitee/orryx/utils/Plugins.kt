package org.gitee.orryx.utils

import org.bukkit.Bukkit
import taboolib.common.platform.function.console
import taboolib.module.lang.sendLang

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

val AdyeshachPlugin = Plugin("Adyeshach")

val DragonCorePlugin = Plugin("DragonCore")

val DragonArmourersPlugin = Plugin("DragonArmourers")

val GermPluginPlugin = Plugin("GermPlugin")

val MythicMobsPlugin = Plugin("MythicMobs")

val RedisChannelPlugin = Plugin("RedisChannel")

val NodensPlugin = Plugin("Nodens")

val AttributePlusPlugin = Plugin("AttributePlus")

val PacketEventsPlugin = Plugin("packetevents")

val ProtocolLibPlugin = Plugin("ProtocolLib")

val GDDTitlePlugin = Plugin("GDDTitle")

val PlaceholderAPIPlugin = Plugin("PlaceholderAPI")

val GlowAPIPlugin = Plugin("GlowAPI")

val DungeonPlusPlugin = Plugin("DungeonPlus")