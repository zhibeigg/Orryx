package org.gitee.orryx.core.profile

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SerializableFlagExtraTest {

    private val json = Json { prettyPrint = false }

    @Nested
    inner class CopyAndDestructuring {
        @Test
        fun `copy with modified field`() {
            val original = SerializableFlag("STRING", "hello", true, 1000)
            val modified = original.copy(value = "world")
            assertEquals("world", modified.value)
            assertEquals(original.type, modified.type)
            assertEquals(original.isPersistence, modified.isPersistence)
            assertEquals(original.timeout, modified.timeout)
        }

        @Test
        fun `destructuring`() {
            val flag = SerializableFlag("INT", "42", false, 500)
            val (type, value, isPersistence, timeout) = flag
            assertEquals("INT", type)
            assertEquals("42", value)
            assertFalse(isPersistence)
            assertEquals(500L, timeout)
        }
    }

    @Nested
    inner class EdgeCases {
        @Test
        fun `empty strings`() {
            val flag = SerializableFlag("", "", false, 0)
            val decoded = json.decodeFromString<SerializableFlag>(json.encodeToString(flag))
            assertEquals(flag, decoded)
        }

        @Test
        fun `large timeout`() {
            val flag = SerializableFlag("LONG", "999", true, Long.MAX_VALUE)
            val decoded = json.decodeFromString<SerializableFlag>(json.encodeToString(flag))
            assertEquals(Long.MAX_VALUE, decoded.timeout)
        }

        @Test
        fun `negative timeout serialization`() {
            val flag = SerializableFlag("INT", "1", false, -1)
            val decoded = json.decodeFromString<SerializableFlag>(json.encodeToString(flag))
            assertEquals(-1L, decoded.timeout)
        }

        @Test
        fun `special characters in value`() {
            val flag = SerializableFlag("STRING", "hello \"world\" \n\t", true, 0)
            val decoded = json.decodeFromString<SerializableFlag>(json.encodeToString(flag))
            assertEquals(flag, decoded)
        }

        @Test
        fun `unicode in value`() {
            val flag = SerializableFlag("STRING", "你好世界🌍", true, 0)
            val decoded = json.decodeFromString<SerializableFlag>(json.encodeToString(flag))
            assertEquals("你好世界🌍", decoded.value)
        }
    }

    @Nested
    inner class Inequality {
        @Test
        fun `different type`() {
            val a = SerializableFlag("STRING", "v", true, 0)
            val b = SerializableFlag("INT", "v", true, 0)
            assertNotEquals(a, b)
        }

        @Test
        fun `different persistence`() {
            val a = SerializableFlag("STRING", "v", true, 0)
            val b = SerializableFlag("STRING", "v", false, 0)
            assertNotEquals(a, b)
        }

        @Test
        fun `different timeout`() {
            val a = SerializableFlag("STRING", "v", true, 100)
            val b = SerializableFlag("STRING", "v", true, 200)
            assertNotEquals(a, b)
        }
    }
}
