package org.gitee.orryx.dao.pojo

import java.util.UUID

data class PlayerSkill(val player: UUID, val job: String, val skill: String, val locked: Boolean, val level: Int, val bindKeyOfGroup: Map<String, String?>)