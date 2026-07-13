package org.gitee.orryx.compat.arcartx.glimmer

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gitee.orryx.utils.getNowOrDefault
import priv.seventeen.artist.arcartx.glimmer.callable.InvocationData
import taboolib.common.platform.function.warning
import java.util.concurrent.CompletableFuture

internal fun InvocationData.stringArgument(index: Int): String? {
    if (index < 0 || size() <= index) return null
    return runCatching { get(index).stringValue() }.getOrNull()
}

internal fun InvocationData.nonBlankStringArgument(index: Int): String? {
    return stringArgument(index)?.takeIf { it.isNotBlank() }
}

internal fun InvocationData.playerArgument(index: Int = 0): Player? {
    return nonBlankStringArgument(index)?.let(Bukkit::getPlayerExact)
}

internal fun InvocationData.finiteDoubleArgument(index: Int): Double? {
    return stringArgument(index)?.toDoubleOrNull()?.takeIf(Double::isFinite)
}

internal fun InvocationData.nonNegativeIntArgument(index: Int): Int? {
    return stringArgument(index)?.toIntOrNull()?.takeIf { it >= 0 }
}

internal fun InvocationData.nonNegativeLongArgument(index: Int): Long? {
    return stringArgument(index)?.toLongOrNull()?.takeIf { it >= 0L }
}

internal fun <T> CompletableFuture<T>.glimmerNow(default: T, operation: String): T {
    monitorGlimmerFailure(operation)
    return getNowOrDefault(default)
}

internal fun <T> glimmerFireAndForget(
    operation: String,
    futureSupplier: () -> CompletableFuture<T>
) {
    try {
        futureSupplier().monitorGlimmerFailure(operation)
    } catch (throwable: Throwable) {
        logGlimmerFailure(operation, throwable)
    }
}

internal fun glimmerOperation(operation: String, block: () -> Unit) {
    try {
        block()
    } catch (throwable: Throwable) {
        logGlimmerFailure(operation, throwable)
    }
}

private fun <T> CompletableFuture<T>.monitorGlimmerFailure(operation: String) {
    whenComplete { _, throwable ->
        if (throwable != null) {
            logGlimmerFailure(operation, throwable)
        }
    }
}

private fun logGlimmerFailure(operation: String, throwable: Throwable) {
    val cause = throwable.cause ?: throwable
    warning("[Orryx] ArcartX-Glimmer $operation 执行失败: ${cause.message ?: cause.javaClass.simpleName}")
}
