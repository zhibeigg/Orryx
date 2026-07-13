package org.gitee.orryx.core.common.timer

import java.util.concurrent.atomic.AtomicLong

class CooldownEntry internal constructor(
    val tag: String,
    initialDuration: Long,
    private val clock: () -> Long
) {

    constructor(tag: String, initialDuration: Long) : this(tag, initialDuration, System::currentTimeMillis)

    private val startStamp = clock()
    private val expiresAt = AtomicLong(expirationFrom(startStamp, initialDuration))

    val remaining: Long
        get() = countdown

    val countdown: Long
        get() = positiveDifference(expiresAt.get(), clock())

    val overStamp: Long
        get() = expiresAt.get()

    val isReady: Boolean
        get() = countdown == 0L

    fun addDuration(amount: Long) {
        if (amount < 0L) {
            reduceDuration(absoluteAmount(amount))
            return
        }
        if (amount == 0L) return
        expiresAt.updateAndGet { current ->
            saturatedAdd(maxOf(current, clock()), amount)
        }
    }

    fun reduceDuration(amount: Long) {
        if (amount < 0L) {
            addDuration(absoluteAmount(amount))
            return
        }
        if (amount == 0L) return
        expiresAt.updateAndGet { current ->
            maxOf(clock(), saturatedSubtract(current, amount))
        }
    }

    companion object {

        internal fun expirationFrom(now: Long, duration: Long): Long {
            return if (duration <= 0L) now else saturatedAdd(now, duration)
        }

        internal fun saturatedAdd(value: Long, amount: Long): Long {
            if (amount <= 0L) return saturatedSubtract(value, absoluteAmount(amount))
            return if (value > Long.MAX_VALUE - amount) Long.MAX_VALUE else value + amount
        }

        internal fun saturatedSubtract(value: Long, amount: Long): Long {
            if (amount <= 0L) return value
            return if (value < Long.MIN_VALUE + amount) Long.MIN_VALUE else value - amount
        }

        internal fun positiveDifference(end: Long, start: Long): Long {
            if (end <= start) return 0L
            return if (start < 0L && end > Long.MAX_VALUE + start) Long.MAX_VALUE else end - start
        }

        private fun absoluteAmount(amount: Long): Long {
            return if (amount == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(amount)
        }
    }
}