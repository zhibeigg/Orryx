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
}
