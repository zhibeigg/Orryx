/**
 * 示例脚本 3: 玩家信息
 *
 * 演示获取和显示玩家信息
 */

import org.bukkit.ChatColor

player?.let { p ->
    // 基本信息
    p.sendMessage("${ChatColor.GOLD}========== 玩家信息 ==========")
    p.sendMessage("${ChatColor.YELLOW}名称: ${ChatColor.WHITE}${p.name}")
    p.sendMessage("${ChatColor.YELLOW}UUID: ${ChatColor.WHITE}${p.uniqueId}")
    p.sendMessage("${ChatColor.YELLOW}显示名称: ${ChatColor.WHITE}${p.displayName}")

    // 位置信息
    val loc = p.location
    p.sendMessage("${ChatColor.YELLOW}世界: ${ChatColor.WHITE}${loc.world?.name}")
    p.sendMessage("${ChatColor.YELLOW}坐标: ${ChatColor.WHITE}X=${loc.blockX}, Y=${loc.blockY}, Z=${loc.blockZ}")

    // 状态信息
    p.sendMessage("${ChatColor.YELLOW}生命值: ${ChatColor.WHITE}${p.health}/${p.maxHealth}")
    p.sendMessage("${ChatColor.YELLOW}饥饿值: ${ChatColor.WHITE}${p.foodLevel}/20")
    p.sendMessage("${ChatColor.YELLOW}等级: ${ChatColor.WHITE}${p.level}")
    p.sendMessage("${ChatColor.YELLOW}经验: ${ChatColor.WHITE}${(p.exp * 100).toInt()}%")

    // 游戏模式
    p.sendMessage("${ChatColor.YELLOW}游戏模式: ${ChatColor.WHITE}${p.gameMode.name}")

    // 权限信息
    p.sendMessage("${ChatColor.YELLOW}是否OP: ${ChatColor.WHITE}${p.isOp}")
    p.sendMessage("${ChatColor.YELLOW}飞行状态: ${ChatColor.WHITE}${p.isFlying}")

    p.sendMessage("${ChatColor.GOLD}============================")

    // 将信息保存到上下文
    context["playerInfo"] = mapOf(
        "name" to p.name,
        "uuid" to p.uniqueId.toString(),
        "location" to "${loc.blockX},${loc.blockY},${loc.blockZ}",
        "health" to p.health,
        "level" to p.level
    )

    "玩家信息已显示"
} ?: run {
    "此脚本需要玩家执行"
}
