package org.gitee.orryx.core.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StringParserTest {

    @Nested
    inner class EntriesParsing {

        @Test
        fun `single selector`() {
            val parser = StringParser("@range 5")
            val entries = parser.entries
            assertEquals(1, entries.size)
            assertEquals("range", entries[0].head)
            assertFalse(entries[0].reverse)
            assertEquals(listOf("5"), entries[0].body)
        }

        @Test
        fun `multiple selectors`() {
            val parser = StringParser("@range 5 @self")
            val entries = parser.entries
            assertEquals(2, entries.size)
            assertEquals("range", entries[0].head)
            assertEquals(listOf("5"), entries[0].body)
            assertEquals("self", entries[1].head)
            assertTrue(entries[1].body.isEmpty())
        }

        @Test
        fun `reverse selector`() {
            val parser = StringParser("!@team")
            val entries = parser.entries
            assertEquals(1, entries.size)
            assertTrue(entries[0].reverse)
            assertEquals("team", entries[0].head)
        }

        @Test
        fun `mixed selectors`() {
            val parser = StringParser("@range 5 !@self @pvp 1")
            val entries = parser.entries
            assertEquals(3, entries.size)
            assertFalse(entries[0].reverse)
            assertEquals("range", entries[0].head)
            assertEquals(listOf("5"), entries[0].body)
            assertTrue(entries[1].reverse)
            assertEquals("self", entries[1].head)
            assertFalse(entries[2].reverse)
            assertEquals("pvp", entries[2].head)
            assertEquals(listOf("1"), entries[2].body)
        }

        @Test
        fun `multiple body params`() {
            val parser = StringParser("@range 5 10 20")
            val entries = parser.entries
            assertEquals(1, entries.size)
            assertEquals(listOf("5", "10", "20"), entries[0].body)
        }

        @Test
        fun `empty string`() {
            val parser = StringParser("")
            val entries = parser.entries
            assertTrue(entries.isEmpty())
        }

        @Test
        fun `text before first selector is ignored`() {
            val parser = StringParser("ignored text @range 5")
            val entries = parser.entries
            assertEquals(1, entries.size)
            assertEquals("range", entries[0].head)
            assertEquals(listOf("5"), entries[0].body)
        }

        @Test
        fun `whitespace trimming`() {
            val parser = StringParser("  @range 5  ")
            val entries = parser.entries
            assertEquals(1, entries.size)
            assertEquals("range", entries[0].head)
        }

        @Test
        fun `complex real-world example`() {
            val parser = StringParser("@range 5 !@self @pvp 1 !@team")
            val entries = parser.entries
            assertEquals(4, entries.size)
            assertEquals("range", entries[0].head)
            assertFalse(entries[0].reverse)
            assertEquals("self", entries[1].head)
            assertTrue(entries[1].reverse)
            assertEquals("pvp", entries[2].head)
            assertFalse(entries[2].reverse)
            assertEquals("team", entries[3].head)
            assertTrue(entries[3].reverse)
        }

        @Test
        fun `entry class properties`() {
            val entry = StringParser.Entry(false, "test", mutableListOf("a", "b"))
            assertFalse(entry.reverse)
            assertEquals("test", entry.head)
            assertEquals(listOf("a", "b"), entry.body)
        }
    }
}
