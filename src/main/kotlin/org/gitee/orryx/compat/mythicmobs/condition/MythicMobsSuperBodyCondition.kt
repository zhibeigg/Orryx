package org.gitee.orryx.compat.mythicmobs.condition

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillCondition
import io.lumine.xikage.mythicmobs.skills.conditions.IEntityCondition
import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import taboolib.common.platform.Ghost

@Ghost
class MythicMobsSuperBodyCondition(line: String, mlc: MythicLineConfig): SkillCondition(line), IEntityCondition {

    override fun check(e: AbstractEntity): Boolean {
        return if (e.isPlayer) {
            Orryx.api().profileAPI.isSuperBody(e.bukkitEntity as Player)
        } else {
            false
        }
    }
}
