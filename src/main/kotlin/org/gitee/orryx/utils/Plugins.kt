package org.gitee.orryx.utils

import org.bukkit.Bukkit

val AdyeshachEnabled: Boolean
    get() = Bukkit.getPluginManager().isPluginEnabled("Adyeshach")

val DragonCoreEnabled: Boolean
    get() = Bukkit.getPluginManager().isPluginEnabled("DragonCore")

val GermPluginEnabled: Boolean
    get() = Bukkit.getPluginManager().isPluginEnabled("GermPlugin")

val MythicMobsEnabled
    get() = Bukkit.getPluginManager().isPluginEnabled("MythicMobs")

val RedisChannelEnabled
    get() = Bukkit.getPluginManager().isPluginEnabled("RedisChannel")

val OriginAttributeEnabled
    get() = Bukkit.getPluginManager().isPluginEnabled("OriginAttribute")

val AttributePlusEnabled
    get() = Bukkit.getPluginManager().isPluginEnabled("AttributePlus")