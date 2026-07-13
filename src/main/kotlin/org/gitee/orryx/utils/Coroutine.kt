package org.gitee.orryx.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.gitee.orryx.api.OrryxAPI
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submit
import taboolib.expansion.AsyncDispatcher
import taboolib.expansion.SyncDispatcher
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
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

/**
 * 在 Orryx I/O 协程域执行阻塞型数据库工作，并以 Future 暴露结果。
 */
fun <T> ioFuture(block: () -> T): CompletableFuture<T> {
    return OrryxAPI.ioScope.future { block() }
}

/**
 * 将 Bukkit 业务切回主线程；当前已在主线程时立即执行。
 */
fun runOnMainThread(block: () -> Unit) {
    if (isPrimaryThread) {
        block()
    } else {
        submit { block() }
    }
}

/**
 * 在 Bukkit 主线程计算结果，不阻塞调用线程。
 */
fun <T> mainThreadFuture(block: () -> T): CompletableFuture<T> {
    val future = CompletableFuture<T>()
    runOnMainThread {
        try {
            future.complete(block())
        } catch (throwable: Throwable) {
            future.completeExceptionally(throwable)
        }
    }
    return future
}

/**
 * 确保 Future 的映射逻辑在 Bukkit 主线程执行。
 */
fun <T, R> CompletionStage<T>.thenApplyMain(block: (T) -> R): CompletableFuture<R> {
    return toCompletableFuture().thenCompose { value -> mainThreadFuture { block(value) } }
}

/**
 * 确保 Future 的组合逻辑在 Bukkit 主线程执行。
 */
fun <T, R> CompletionStage<T>.thenComposeMain(block: (T) -> CompletionStage<R>): CompletableFuture<R> {
    return toCompletableFuture().thenCompose { value ->
        mainThreadFuture { block(value) }.thenCompose { it }
    }
}

/**
 * 非阻塞读取已完成 Future；未完成、取消或异常时返回默认值。
 */
fun <T> CompletableFuture<T>.getNowOrDefault(default: T): T {
    return if (isDone && !isCompletedExceptionally && !isCancelled) getNow(default) else default
}
