package org.gitee.orryx.utils

import org.bukkit.ChatColor
import org.bukkit.ChatColor.COLOR_CHAR
import kotlin.math.min

val reader = VariableReader()

fun getColorsAtPosition(text: String, position: Int): Set<ChatColor> {
    val currentStyles = mutableSetOf<ChatColor>()
    val chars = text.toCharArray()
    var i = 0
    while (i < min(position.toDouble(), chars.size.toDouble())) {
        if (chars[i] == COLOR_CHAR && i + 1 < chars.size) {
            val color = ChatColor.getByChar(chars[i + 1])
            if (color != null) {
                if (color.isColor) { // 颜色类型（如 §a）
                    currentStyles.removeIf { obj: ChatColor -> obj.isColor }  // 移除旧颜色
                    currentStyles.add(color)
                } else if (color.isFormat) { // 格式类型（如 §l）
                    currentStyles.add(color)
                } else if (color === ChatColor.RESET) { // 重置样式
                    currentStyles.clear()
                }
            }
            i++ // 跳过下一个字符（已处理）
        }
        i++
    }
    return currentStyles
}

fun getFormatAtPosition(text: String, position: Int): String {
    val builder = StringBuilder()
    getColorsAtPosition(text, position).forEach {
        builder.append(COLOR_CHAR)
        builder.append(it.char)
    }
    return builder.toString()
}