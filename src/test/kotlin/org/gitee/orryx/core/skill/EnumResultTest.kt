package org.gitee.orryx.core.skill

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EnumResultTest {

    @Nested
    inner class CastResultTest {
        @Test
        fun `all values exist`() {
            val expected = setOf(
                "SUCCESS", "PARAMETER", "MANA_NOT_ENOUGH", "CHECK_ACTION_FAILED",
                "SILENCE", "COOLDOWN", "CANCELED", "PASSIVE", "PRESSING", "AIMING"
            )
            assertEquals(expected, CastResult.entries.map { it.name }.toSet())
        }

        @Test
        fun `valueOf roundtrip`() {
            CastResult.entries.forEach {
                assertEquals(it, CastResult.valueOf(it.name))
            }
        }

        @Test
        fun `ordinal is stable`() {
            assertEquals(0, CastResult.SUCCESS.ordinal)
            assertEquals(1, CastResult.PARAMETER.ordinal)
        }

        @Test
        fun `count is 10`() {
            assertEquals(10, CastResult.entries.size)
        }
    }

    @Nested
    inner class SkillLevelResultTest {
        @Test
        fun `all values exist`() {
            val expected = setOf(
                "CANCELLED", "MAX", "MIN", "SAME", "NONE", "POINT", "POINT_REFUND", "CHECK", "SUCCESS"
            )
            assertEquals(expected, SkillLevelResult.entries.map { it.name }.toSet())
        }

        @Test
        fun `valueOf roundtrip`() {
            SkillLevelResult.entries.forEach {
                assertEquals(it, SkillLevelResult.valueOf(it.name))
            }
        }

        @Test
        fun `count is 9`() {
            assertEquals(9, SkillLevelResult.entries.size)
        }
    }
}
