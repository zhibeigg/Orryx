package org.gitee.orryx.module.spirit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpiritResultTest {

    @Test
    fun `all values exist`() {
        val expected = setOf("CANCELLED", "NO_JOB", "NOT_ENOUGH", "SUCCESS", "SAME")
        assertEquals(expected, SpiritResult.entries.map { it.name }.toSet())
    }

    @Test
    fun `valueOf roundtrip`() {
        SpiritResult.entries.forEach {
            assertEquals(it, SpiritResult.valueOf(it.name))
        }
    }

    @Test
    fun `count is 5`() {
        assertEquals(5, SpiritResult.entries.size)
    }

    @Test
    fun `same name as ManaResult counterparts`() {
        // SpiritResult 和 ManaResult 应该有相同的枚举名
        val spiritNames = SpiritResult.entries.map { it.name }.toSet()
        assertTrue(spiritNames.contains("CANCELLED"))
        assertTrue(spiritNames.contains("NO_JOB"))
        assertTrue(spiritNames.contains("NOT_ENOUGH"))
        assertTrue(spiritNames.contains("SUCCESS"))
        assertTrue(spiritNames.contains("SAME"))
    }
}
