package org.gitee.orryx.core.skill

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CastResultExtendedTest {

    @Nested
    inner class EnumCompleteness {
        @Test
        fun `exactly 10 values`() {
            assertEquals(10, CastResult.entries.size)
        }

        @Test
        fun `all expected values exist`() {
            val expected = setOf(
                "SUCCESS", "PARAMETER", "MANA_NOT_ENOUGH", "CHECK_ACTION_FAILED",
                "SILENCE", "COOLDOWN", "CANCELED", "PASSIVE", "PRESSING", "AIMING"
            )
            val actual = CastResult.entries.map { it.name }.toSet()
            assertEquals(expected, actual)
        }

        @Test
        fun `valueOf round-trip for all values`() {
            CastResult.entries.forEach { result ->
                assertEquals(result, CastResult.valueOf(result.name))
            }
        }

        @Test
        fun `invalid valueOf throws`() {
            assertThrows(IllegalArgumentException::class.java) {
                CastResult.valueOf("INVALID")
            }
        }
    }

    @Nested
    inner class OrdinalStability {
        @Test
        fun `SUCCESS is first`() {
            assertEquals(0, CastResult.SUCCESS.ordinal)
        }

        @Test
        fun `AIMING is last`() {
            assertEquals(9, CastResult.AIMING.ordinal)
        }

        @Test
        fun `ordinals are sequential`() {
            CastResult.entries.forEachIndexed { index, result ->
                assertEquals(index, result.ordinal)
            }
        }
    }

    @Nested
    inner class CategoryClassification {
        @Test
        fun `failure results are distinct from success`() {
            val failures = setOf(
                CastResult.PARAMETER, CastResult.MANA_NOT_ENOUGH,
                CastResult.CHECK_ACTION_FAILED, CastResult.SILENCE,
                CastResult.COOLDOWN, CastResult.CANCELED
            )
            assertFalse(failures.contains(CastResult.SUCCESS))
        }

        @Test
        fun `special state results`() {
            val specialStates = setOf(CastResult.PASSIVE, CastResult.PRESSING, CastResult.AIMING)
            assertEquals(3, specialStates.size)
        }
    }

    @Nested
    inner class SetAndMapUsage {
        @Test
        fun `can be used in Set`() {
            val set = setOf(CastResult.SUCCESS, CastResult.SUCCESS, CastResult.COOLDOWN)
            assertEquals(2, set.size)
        }

        @Test
        fun `can be used as Map key`() {
            val map = mapOf(CastResult.SUCCESS to "ok", CastResult.COOLDOWN to "wait")
            assertEquals("ok", map[CastResult.SUCCESS])
            assertEquals("wait", map[CastResult.COOLDOWN])
        }
    }
}
