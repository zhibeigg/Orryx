package org.gitee.orryx.core.common

class CooldownEntry(
    val tag: String,
    initialDuration: Long
) {

    private val timeStamp = System.currentTimeMillis()

    var remaining = initialDuration
        private set

    val countdown: Long
        get() = (remaining + timeStamp - System.currentTimeMillis()).coerceAtLeast(0)

    val isReady: Boolean
        get() = countdown <= 0

    fun addDuration(amount: Long) {
        remaining += amount
        if (remaining < 0) remaining = 0
    }

    fun reduceDuration(amount: Long) {
        remaining -= amount
        if (remaining < 0) remaining = 0
    }

}