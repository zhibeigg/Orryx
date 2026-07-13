package org.gitee.orryx.core.kether.actions.game.projectile

import java.util.concurrent.atomic.AtomicBoolean

internal class SingleFlightLifecycle {
    private val active = AtomicBoolean(true)
    private val inFlight = AtomicBoolean(false)

    val isActive: Boolean
        get() = active.get()

    fun tryStart(): Boolean {
        if (!active.get() || !inFlight.compareAndSet(false, true)) return false
        if (active.get()) return true
        inFlight.set(false)
        return false
    }

    fun finish() {
        inFlight.set(false)
    }

    fun close(): Boolean {
        return active.getAndSet(false)
    }
}
