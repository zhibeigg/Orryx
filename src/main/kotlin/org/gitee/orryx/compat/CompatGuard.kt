package org.gitee.orryx.compat

import taboolib.common.platform.function.warning

/** 可选依赖缺类或二进制不兼容时降级，而不是阻止 Orryx 启动。 */
object CompatGuard {

    inline fun <T> linkageFallback(name: String, fallback: () -> T, block: () -> T): T {
        return try {
            block()
        } catch (error: LinkageError) {
            warning("兼容模块 $name 加载失败，已降级: ${error.message ?: error.javaClass.simpleName}")
            fallback()
        }
    }

    inline fun <T> firstAvailable(default: () -> T, vararg candidates: Pair<() -> Boolean, () -> T>): T {
        candidates.forEach { (available, factory) ->
            val enabled = linkageFallback("可选依赖检测", { false }, available)
            if (enabled) {
                return linkageFallback("可选依赖桥接", default, factory)
            }
        }
        return default()
    }
}
