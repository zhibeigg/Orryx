package org.gitee.orryx.core.common

import kotlin.math.max

open class Baffle(val tag: String, private var timeout: Long) {

    private var timeStamp = System.currentTimeMillis()

    private val endTimeStamp: Long
        get() = timeStamp + timeout

    // 当前时间 大于 结束时间
    val next: Boolean
        get() = countdown == 0L

    private val currentTimeStamp: Long
        get() = System.currentTimeMillis()

    val countdown: Long
        get() = max(endTimeStamp - currentTimeStamp, 0L)

    fun clear() {
        timeStamp = 0L
        timeout = 0L
    }

    fun reduce(stamp: Long) {
        timeout -= stamp
    }

    fun increase(stamp: Long) {
        timeout += stamp
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Baffle) return false
        return tag == other.tag
    }

    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + timeout.hashCode()
        result = 31 * result + timeStamp.hashCode()
        return result
    }

}