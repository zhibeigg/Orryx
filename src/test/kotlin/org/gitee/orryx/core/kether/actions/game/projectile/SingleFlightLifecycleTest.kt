package org.gitee.orryx.core.kether.actions.game.projectile

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SingleFlightLifecycleTest {

    @Test
    fun `only one cycle may be in flight`() {
        val lifecycle = SingleFlightLifecycle()

        assertTrue(lifecycle.tryStart())
        assertFalse(lifecycle.tryStart())

        lifecycle.finish()
        assertTrue(lifecycle.tryStart())
    }

    @Test
    fun `zero timeout still allows the initial cycle`() {
        val lifetime = ProjectileLifetime(period = 1, timeout = 0)
        assertTrue(lifetime.advance())
        assertFalse(lifetime.advance())
    }

    @Test
    fun `timeout advances even while a cycle remains in flight`() {
        val lifecycle = SingleFlightLifecycle()
        val lifetime = ProjectileLifetime(period = 0, timeout = 3)
        assertTrue(lifecycle.tryStart())

        assertTrue(lifetime.advance())
        assertTrue(lifetime.advance())
        assertTrue(lifetime.advance())
        assertTrue(lifetime.advance())
        assertFalse(lifetime.advance())
        assertFalse(lifecycle.tryStart())
    }

    @Test
    fun `closed lifecycle rejects late and future cycles`() {
        val lifecycle = SingleFlightLifecycle()
        assertTrue(lifecycle.tryStart())

        assertTrue(lifecycle.close())
        lifecycle.finish()

        assertFalse(lifecycle.isActive)
        assertFalse(lifecycle.tryStart())
        assertFalse(lifecycle.close())
    }
}
