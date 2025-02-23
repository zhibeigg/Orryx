package org.gitee.orryx.core.common

class CooldownEntry(
    val tag: String,
    initialDuration: Long
) {

    private val timeStamp = System.currentTimeMillis()

    var remaining = initialDuration
        private set

    val countdown: Long
        get() = remaining - (System.currentTimeMillis() - timeStamp)

    val isReady: Boolean
        get() = remaining <= 0

    fun addDuration(amount: Long) {
        remaining += amount
        if (remaining < 0) remaining = 0
    }

    fun reduceDuration(amount: Long) {
        remaining -= amount
        if (remaining < 0) remaining = 0
    }

}