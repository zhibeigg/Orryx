package org.gitee.orryx.utils

import kotlinx.coroutines.Dispatchers
import taboolib.common.platform.function.isPrimaryThread
import taboolib.expansion.AsyncDispatcher
import taboolib.expansion.SyncDispatcher
import kotlin.IllegalArgumentException
import kotlin.coroutines.CoroutineContext

/**
 * Minecraft async dispatcher.
 */
val Dispatchers.minecraftAsync: CoroutineContext
    get() = AsyncDispatcher

/**
 * Minecraft sync dispatcher.
 */
val Dispatchers.minecraftMain: CoroutineContext
    get() = SyncDispatcher

/**
* @throws [IllegalArgumentException] 如果在主线程运行
* */
inline fun <T> requireAsync(name: String, function: () -> T): T {
    if (isPrimaryThread) throw IllegalArgumentException("禁止在主线程运行 $name 方法")
    return function()
}

fun requireAsync(name: String) {
    if (isPrimaryThread) throw IllegalArgumentException("禁止在主线程运行 $name 方法")
}