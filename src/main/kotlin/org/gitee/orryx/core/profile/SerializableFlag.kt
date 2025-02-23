package org.gitee.orryx.core.profile

import kotlinx.serialization.Serializable

@Serializable
data class SerializableFlag(val type: String, val value: String, val isPersistence: Boolean, val timeout: Long)