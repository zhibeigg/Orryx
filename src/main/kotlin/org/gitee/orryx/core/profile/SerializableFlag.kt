package org.gitee.orryx.core.profile

import kotlinx.serialization.Serializable

@Serializable
data class SerializableFlag(
    val type: String,
    val value: String,
    val isPersistence: Boolean,
    val timeout: Long,
    val expiresAt: Long = 0L,
) {
    constructor(type: String, value: String, isPersistence: Boolean, timeout: Long) :
        this(type, value, isPersistence, timeout, 0L)
}