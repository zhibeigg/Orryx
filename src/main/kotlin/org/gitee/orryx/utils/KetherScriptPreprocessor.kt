package org.gitee.orryx.utils

import java.nio.charset.StandardCharsets

/**
 * Orryx Kether 源码的统一预处理器。
 *
 * 在识别完整 `def` 脚本前先移除引号外的 `#` 注释，并保留原始换行，
 * 使配置脚本、临时脚本和表达式入口共享一致的注释语义。
 */
internal object KetherScriptPreprocessor {

    fun stripComments(source: String): String {
        val result = StringBuilder(source.length)
        var quote: Char? = null
        var escaped = false
        var comment = false

        source.forEach { char ->
            if (comment) {
                if (char == '\r' || char == '\n') {
                    result.append(char)
                    comment = false
                    escaped = false
                }
                return@forEach
            }

            if (char == '#' && quote == null && !escaped) {
                comment = true
                escaped = false
                return@forEach
            }

            result.append(char)
            when {
                char == '\r' || char == '\n' -> escaped = false
                char == '\\' -> escaped = !escaped
                else -> {
                    if (!escaped && (char == '"' || char == '\'')) {
                        quote = if (quote == char) null else quote ?: char
                    }
                    escaped = false
                }
            }
        }
        return result.toString()
    }

    fun prepareScript(source: String): String {
        val uncommented = stripComments(source)
        return if (startsWithDefinition(uncommented)) {
            uncommented
        } else {
            "def main = { $uncommented }"
        }
    }

    private fun startsWithDefinition(source: String): Boolean {
        val start = source.indexOfFirst { !it.isWhitespace() }
        if (start < 0 || !source.regionMatches(start, "def", 0, 3)) return false
        return source.getOrNull(start + 3)?.isWhitespace() != false
    }
}

internal fun stripKetherComments(actions: String): String {
    return KetherScriptPreprocessor.stripComments(actions)
}

internal fun getBytes(actions: String): ByteArray {
    return KetherScriptPreprocessor.prepareScript(actions).toByteArray(StandardCharsets.UTF_8)
}
