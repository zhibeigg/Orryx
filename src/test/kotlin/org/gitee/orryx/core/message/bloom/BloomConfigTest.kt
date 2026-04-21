package org.gitee.orryx.core.message.bloom

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BloomConfigTest {

    @Test
    fun `data class properties`() {
        val config = BloomConfig("bloom1", "Fire Bloom", 255, 128, 0, 200, 1.5f, 10.0f, 1)
        assertEquals("bloom1", config.id)
        assertEquals("Fire Bloom", config.name)
        assertEquals(255, config.r)
        assertEquals(128, config.g)
        assertEquals(0, config.b)
        assertEquals(200, config.a)
        assertEquals(1.5f, config.strength)
        assertEquals(10.0f, config.radius)
        assertEquals(1, config.priority)
    }

    @Test
    fun `data class equality`() {
        val a = BloomConfig("id", "name", 1, 2, 3, 4, 0.5f, 1.0f, 0)
        val b = BloomConfig("id", "name", 1, 2, 3, 4, 0.5f, 1.0f, 0)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `data class inequality`() {
        val a = BloomConfig("id1", "name", 1, 2, 3, 4, 0.5f, 1.0f, 0)
        val b = BloomConfig("id2", "name", 1, 2, 3, 4, 0.5f, 1.0f, 0)
        assertNotEquals(a, b)
    }

    @Test
    fun `copy with modified field`() {
        val original = BloomConfig("id", "name", 255, 255, 255, 255, 1.0f, 5.0f, 0)
        val modified = original.copy(strength = 2.0f)
        assertEquals(2.0f, modified.strength)
        assertEquals(original.id, modified.id)
    }

    @Test
    fun `toString contains all fields`() {
        val config = BloomConfig("test", "Test", 0, 0, 0, 0, 0f, 0f, 0)
        val str = config.toString()
        assertTrue(str.contains("test"))
        assertTrue(str.contains("Test"))
    }

    @Test
    fun `destructuring`() {
        val config = BloomConfig("id", "name", 1, 2, 3, 4, 0.5f, 1.0f, 10)
        val (id, name, r, g, b, a, strength, radius, priority) = config
        assertEquals("id", id)
        assertEquals("name", name)
        assertEquals(1, r)
        assertEquals(2, g)
        assertEquals(3, b)
        assertEquals(4, a)
        assertEquals(0.5f, strength)
        assertEquals(1.0f, radius)
        assertEquals(10, priority)
    }

    @Test
    fun `zero values`() {
        val config = BloomConfig("", "", 0, 0, 0, 0, 0f, 0f, 0)
        assertEquals(0, config.r)
        assertEquals(0f, config.strength)
    }

    @Test
    fun `negative priority`() {
        val config = BloomConfig("id", "name", 0, 0, 0, 0, 0f, 0f, -1)
        assertEquals(-1, config.priority)
    }
}
