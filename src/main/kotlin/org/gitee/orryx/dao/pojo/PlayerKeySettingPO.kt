package org.gitee.orryx.dao.pojo

import kotlinx.serialization.Serializable

@Serializable
data class PlayerKeySettingPO(
    val bindKeyMap: Map<String, String>,
    val aimConfirmKey: String,
    val aimCancelKey: String,
    val generalAttackKey: String,
    val blockKey: String,
    val dodgeKey: String,
    val extKeyMap: Map<String, String>
)