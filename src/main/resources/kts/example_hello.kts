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

// 获取玩家名称
val playerName = player?.name ?: "Console"

// 发送彩色消息
player?.sendMessage("${ChatColor.GREEN}Hello, $playerName!")
player?.sendMessage("${ChatColor.YELLOW}这是一个 KTS 脚本示例")

// 使用上下文变量
val customMessage = context["message"] as? String ?: "欢迎使用 Orryx 脚本系统!"
player?.sendMessage(customMessage)

// 设置返回值供外部使用
context["executed"] = true
context["timestamp"] = System.currentTimeMillis()

// 脚本返回值
"Hello World 脚本执行成功"
