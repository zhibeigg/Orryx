package org.gitee.orryx.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Color.kt 工具函数测试。
 * 注意：getColorsAtPosition/getFormatAtPosition 依赖 Bukkit ChatColor 运行时，
 * 仅测试不触发 ChatColor 类加载的边界情况。
 */
class ColorUtilTest {

    @Nested
    inner class GetColorsAtPosition {
        @Test
        fun `empty string returns empty set`() {
            val result = getColorsAtPosition("", 0)
            assertTrue(result.isEmpty())
        }

        @Test
        fun `plain text returns empty set`() {
            val result = getColorsAtPosition("Hello World", 11)
            assertTrue(result.isEmpty())
        }

        @Test
        fun `position 0 returns empty set`() {
            val result = getColorsAtPosition("any text", 0)
            assertTrue(result.isEmpty())
        }

        @Test
        fun `text without color char returns empty`() {
            val result = getColorsAtPosition("no colors here", 14)
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class GetFormatAtPosition {
        @Test
        fun `empty string returns empty format`() {
            assertEquals("", getFormatAtPosition("", 0))
        }

        @Test
        fun `plain text returns empty format`() {
            assertEquals("", getFormatAtPosition("Hello", 5))
        }

        @Test
        fun `position 0 returns empty format`() {
            assertEquals("", getFormatAtPosition("test", 0))
        }
    }

    @Nested
    inner class ReaderInstance {
        @Test
        fun `reader is initialized`() {
            assertNotNull(reader)
        }

        @Test
        fun `reader is a VariableReader`() {
            assertTrue(reader is VariableReader)
        }
    }
}
