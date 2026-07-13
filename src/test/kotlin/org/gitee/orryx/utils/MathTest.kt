package org.gitee.orryx.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MathTest {

    @Test fun `square of positive`() {
        assertEquals(4.0, square(2.0))
    }

    @Test fun `square of zero`() {
        assertEquals(0.0, square(0.0))
    }

    @Test fun `square of negative`() {
        assertEquals(9.0, square(-3.0))
    }

    @Test fun `square of fraction`() {
        assertEquals(0.25, square(0.5), 1e-15)
    }

    @Test fun `tick conversion preserves regular signed values`() {
        assertEquals(1_000L, ticksToMillisSaturated(20L))
        assertEquals(-1_000L, ticksToMillisSaturated(-20L))
    }

    @Test fun `tick conversion saturates at long boundaries`() {
        assertEquals(Long.MAX_VALUE, ticksToMillisSaturated(Long.MAX_VALUE))
        assertEquals(Long.MIN_VALUE, ticksToMillisSaturated(Long.MIN_VALUE))
    }
}
