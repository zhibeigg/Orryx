package org.gitee.orryx.dao.pojo

import kotlinx.serialization.Serializable
import org.gitee.orryx.core.profile.SerializableFlag
import org.gitee.orryx.dao.serializer.UUIDSerializer
import java.util.*

@Serializable
data class PlayerProfilePO(
    val id: Int,
    @Serializable(with = UUIDSerializer::class)
    val player: UUID,
    val job: String?,
    val point: Int,
    val flags: Map<String, SerializableFlag>
)