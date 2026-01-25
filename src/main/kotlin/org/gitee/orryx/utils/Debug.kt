package org.gitee.orryx.utils

import org.gitee.orryx.api.Orryx
import taboolib.common.platform.function.console
import taboolib.module.chat.colored
import taboolib.module.configuration.util.ReloadAwareLazy

val debug: Boolean by ReloadAwareLazy(Orryx.config) { Orryx.config.getBoolean("Debug") }

/**
 * 高性能调试日志
 * 使用 inline + lambda 实现零开销：当 debug 关闭时，lambda 不会被执行
 *
 * @param message 延迟计算的消息 lambda
 */
inline fun debug(message: () -> String) {
    if (debug) consoleMessage(message())
}

/**
 * 高性能调试日志（多条消息）
 * 使用 inline + lambda 实现零开销：当 debug 关闭时，lambda 不会被执行
 *
 * @param messages 延迟计算的消息列表 lambda
 */
inline fun debugMultiple(messages: () -> List<String>) {
    if (debug) messages().forEach { consoleMessage(it) }
}

fun consoleMessage(vararg messages: String) {
    for (message in messages) {
        console().sendMessage("[Orryx] $message".colored())
    }
}