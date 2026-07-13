package org.gitee.orryx.compat

import taboolib.common.platform.function.warning

/** 可选依赖缺类或二进制不兼容时降级，而不是阻止 Orryx 启动。 */
object CompatGuard {

    inline fun <T> linkageFallback(name: String, fallback: () -> T, block: () -> T): T {
        return try {
            block()
        } catch (error: LinkageError) {
            report(name, error)
            fallback()
        }
    }

    fun <T> firstAvailable(default: () -> T, vararg candidates: Pair<() -> Boolean, () -> T>): T {
        return firstAvailable(default, candidates) { name, error -> report(name, error) }
    }

    internal fun <T> firstAvailable(
        default: () -> T,
        candidates: Array<out Pair<() -> Boolean, () -> T>>,
        onLinkageError: (String, LinkageError) -> Unit,
    ): T {
        candidates.forEach { (available, factory) ->
            val enabled = try {
                available()
            } catch (error: LinkageError) {
                onLinkageError("可选依赖检测", error)
                false
            }
            if (!enabled) return@forEach

            try {
                return factory()
            } catch (error: LinkageError) {
                onLinkageError("可选依赖桥接", error)
            }
        }
        return default()
    }

    internal fun <T> degradeOnce(name: String, initial: T, fallback: T): OneTimeLinkageFallback<T> {
        return OneTimeLinkageFallback(initial, fallback) { error -> report(name, error) }
    }

    @PublishedApi
    internal fun report(name: String, error: LinkageError) {
        warning("兼容模块 $name 加载失败，已降级: ${error.message ?: error.javaClass.simpleName}")
    }
}

internal class OneTimeLinkageFallback<T>(
    initial: T,
    private val fallback: T,
    private val onLinkageError: (LinkageError) -> Unit,
) {

    @Volatile
    private var current = initial

    fun current(): T = current

    fun <R> invoke(block: (T) -> R): R {
        val active = current
        if (active === fallback) return block(active)

        return try {
            block(active)
        } catch (error: LinkageError) {
            val shouldReport = synchronized(this) {
                if (current === fallback) {
                    false
                } else {
                    current = fallback
                    true
                }
            }
            if (shouldReport) onLinkageError(error)
            block(fallback)
        }
    }
}
