package org.gitee.orryx.utils

fun square(x: Double): Double {
    return x * x
}

/** 将 Minecraft tick 转为毫秒，并在 Long 边界饱和而不是溢出反号。 */
fun ticksToMillisSaturated(ticks: Long): Long {
    return when {
        ticks > Long.MAX_VALUE / 50L -> Long.MAX_VALUE
        ticks < Long.MIN_VALUE / 50L -> Long.MIN_VALUE
        else -> ticks * 50L
    }
}