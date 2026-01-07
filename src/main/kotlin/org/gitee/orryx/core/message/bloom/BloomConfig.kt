package org.gitee.orryx.core.message.bloom

data class BloomConfig(
    val id: String,
    val name: String,
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int,
    val strength: Float,
    val radius: Float,
    val priority: Int
)
