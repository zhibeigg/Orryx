package org.gitee.orryx.module.state

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MoveStateTest {

    @Nested
    inner class EnumValues {
        @Test
        fun `FRONT maps to W`() {
            assertEquals("W", MoveState.FRONT.key)
        }

        @Test
        fun `REAR maps to S`() {
            assertEquals("S", MoveState.REAR.key)
        }

        @Test
        fun `LEFT maps to A`() {
            assertEquals("A", MoveState.LEFT.key)
        }

        @Test
        fun `RIGHT maps to D`() {
            assertEquals("D", MoveState.RIGHT.key)
        }
    }

    @Nested
    inner class EnumCompleteness {
        @Test
        fun `exactly 4 values`() {
            assertEquals(4, MoveState.entries.size)
        }

        @Test
        fun `valueOf round-trip`() {
            MoveState.entries.forEach { state ->
                assertEquals(state, MoveState.valueOf(state.name))
            }
        }

        @Test
        fun `all keys are unique`() {
            val keys = MoveState.entries.map { it.key }
            assertEquals(keys.size, keys.toSet().size)
        }

        @Test
        fun `all keys are single uppercase letter`() {
            MoveState.entries.forEach { state ->
                assertEquals(1, state.key.length)
                assertTrue(state.key[0].isUpperCase())
            }
        }

        @Test
        fun `ordinal stability`() {
            assertEquals(0, MoveState.FRONT.ordinal)
            assertEquals(1, MoveState.REAR.ordinal)
            assertEquals(2, MoveState.LEFT.ordinal)
            assertEquals(3, MoveState.RIGHT.ordinal)
        }
    }

    @Nested
    inner class KeyMapping {
        @Test
        fun `WASD keys cover standard movement`() {
            val keys = MoveState.entries.map { it.key }.toSet()
            assertTrue(keys.containsAll(setOf("W", "A", "S", "D")))
        }

        @Test
        fun `invalid valueOf throws`() {
            assertThrows(IllegalArgumentException::class.java) {
                MoveState.valueOf("INVALID")
            }
        }
    }
}
