/**
 * 示例脚本 1: Hello World
 *
 * 演示基本的脚本功能:
 * - 访问插件实例
 * - 访问玩家对象
 * - 使用上下文变量
 * - 发送消息
 */

import org.bukkit.ChatColor
import taboolib.platform.util.onlinePlayers

// 发送彩色消息
onlinePlayers.forEach {
    it.sendMessage("${ChatColor.GREEN}Hello, ${it.name}!")
    it.sendMessage("${ChatColor.YELLOW}这是一个 KTS 脚本示例")
}

// 脚本返回值
"Hello World 脚本执行成功"
