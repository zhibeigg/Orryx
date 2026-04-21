package org.gitee.orryx.module.mana

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ManaResultTest {

    @Test
    fun `all values exist`() {
        val expected = setOf("CANCELLED", "NO_JOB", "NOT_ENOUGH", "SUCCESS", "SAME")
        assertEquals(expected, ManaResult.entries.map { it.name }.toSet())
    }

    @Test
    fun `valueOf roundtrip`() {
        ManaResult.entries.forEach {
            assertEquals(it, ManaResult.valueOf(it.name))
        }
    }

    @Test
    fun `count is 5`() {
        assertEquals(5, ManaResult.entries.size)
    }
}
