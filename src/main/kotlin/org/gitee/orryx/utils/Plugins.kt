package org.gitee.orryx.utils

import org.bukkit.Bukkit

val AdyeshachEnabled: Boolean
    get() = Bukkit.getPluginManager().isPluginEnabled("Adyeshach")

val DragonCoreEnabled: Boolean
    get() = Bukkit.getPluginManager().isPluginEnabled("DragonCore")

val MythicMobsEnabled
    get() = Bukkit.getPluginManager().isPluginEnabled("MythicMobs")

val RedisChannelEnabled
    get() = Bukkit.getPluginManager().isPluginEnabled("RedisChannel")