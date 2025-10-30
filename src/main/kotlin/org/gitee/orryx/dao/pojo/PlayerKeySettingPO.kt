package org.gitee.orryx.dao.pojo

import kotlinx.serialization.Serializable
import org.gitee.orryx.dao.serializer.UUIDSerializer
import java.util.UUID

@Serializable
data class PlayerKeySettingPO(
    val id: Int,
    @param:Serializable(with = UUIDSerializer::class)
    val player: UUID,
    val bindKeyMap: Map<String, String>,
    val aimConfirmKey: String,
    val aimCancelKey: String,
    val generalAttackKey: String,
    val blockKey: String,
    val dodgeKey: String,
    val extKeyMap: Map<String, String>
)