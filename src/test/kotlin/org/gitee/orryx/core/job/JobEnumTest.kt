package org.gitee.orryx.core.job

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class JobEnumTest {

    @Nested
    inner class ExperienceResultTest {
        @Test
        fun `all values exist`() {
            val expected = setOf("CANCELLED", "SUCCESS", "SAME")
            assertEquals(expected, ExperienceResult.entries.map { it.name }.toSet())
        }

        @Test
        fun `valueOf roundtrip`() {
            ExperienceResult.entries.forEach {
                assertEquals(it, ExperienceResult.valueOf(it.name))
            }
        }

        @Test
        fun `count is 3`() {
            assertEquals(3, ExperienceResult.entries.size)
        }
    }

    @Nested
    inner class LevelResultTest {
        @Test
        fun `all values exist`() {
            val expected = setOf("CANCELLED", "SUCCESS", "SAME")
            assertEquals(expected, LevelResult.entries.map { it.name }.toSet())
        }

        @Test
        fun `valueOf roundtrip`() {
            LevelResult.entries.forEach {
                assertEquals(it, LevelResult.valueOf(it.name))
            }
        }

        @Test
        fun `count is 3`() {
            assertEquals(3, LevelResult.entries.size)
        }
    }
}
