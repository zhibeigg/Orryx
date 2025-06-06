package org.gitee.orryx.compat.mythicmobs.condition

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillCondition
import io.lumine.xikage.mythicmobs.skills.conditions.IEntityCondition
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderString
import org.bukkit.entity.Player
import org.gitee.orryx.utils.orryxProfile
import taboolib.common.platform.Ghost
import taboolib.module.kether.orNull

@Ghost
class MythicMobsJobCondition(line: String, mlc: MythicLineConfig): SkillCondition(line), IEntityCondition {

    val job: PlaceholderString? = mlc.getPlaceholderString("job", null)

    override fun check(e: AbstractEntity): Boolean {
        return if (e.isPlayer) {
            val profile = (e.bukkitEntity as Player).orryxProfile().orNull() ?: return false
            job?.get(e) == profile.job
        } else {
            false
        }
    }
}