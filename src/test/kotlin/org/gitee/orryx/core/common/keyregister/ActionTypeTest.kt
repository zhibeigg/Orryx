package org.gitee.orryx.core.common.keyregister

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ActionTypeTest {

    @Nested
    inner class EnumValues {
        @Test
        fun `PRESS exists`() {
            assertNotNull(IKeyRegister.ActionType.PRESS)
        }

        @Test
        fun `RELEASE exists`() {
            assertNotNull(IKeyRegister.ActionType.RELEASE)
        }

        @Test
        fun `exactly 2 values`() {
            assertEquals(2, IKeyRegister.ActionType.entries.size)
        }

        @Test
        fun `valueOf round-trip`() {
            IKeyRegister.ActionType.entries.forEach { type ->
                assertEquals(type, IKeyRegister.ActionType.valueOf(type.name))
            }
        }

        @Test
        fun `ordinal stability`() {
            assertEquals(0, IKeyRegister.ActionType.PRESS.ordinal)
            assertEquals(1, IKeyRegister.ActionType.RELEASE.ordinal)
        }

        @Test
        fun `invalid valueOf throws`() {
            assertThrows(IllegalArgumentException::class.java) {
                IKeyRegister.ActionType.valueOf("CLICK")
            }
        }
    }
}
