package org.gitee.orryx.dao.pojo

import kotlinx.serialization.Serializable
import org.gitee.orryx.dao.serializer.UUIDSerializer
import java.util.*

@Serializable
data class PlayerSkillPO(
    val id: Int,
    @param:Serializable(with = UUIDSerializer::class)
    val player: UUID,
    val job: String,
    val skill: String,
    val locked: Boolean,
    val level: Int
)