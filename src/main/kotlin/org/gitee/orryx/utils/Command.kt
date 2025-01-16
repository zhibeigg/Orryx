package org.gitee.orryx.utils

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandContext

fun <T> CommandContext<T>.bukkitPlayer(): Player? {
    return Bukkit.getPlayerExact(get("player"))
}