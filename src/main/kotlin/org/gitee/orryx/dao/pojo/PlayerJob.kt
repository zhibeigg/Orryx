package org.gitee.orryx.dao.pojo

import kotlinx.serialization.Serializable
import org.gitee.orryx.dao.serializer.UUIDSerializer
import java.util.*

@Serializable
data class PlayerJob(
    @Serializable(with = UUIDSerializer::class)
    val player: UUID,
    val job: String,
    val experience: Int,
    val group: String,
    val bindKeyOfGroup: Map<String, Map<String, String?>>
)