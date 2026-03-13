package org.gitee.orryx.compat.mythicmobs.condition

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillCondition
import io.lumine.xikage.mythicmobs.skills.conditions.IEntityCondition
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderString
import io.lumine.xikage.mythicmobs.utils.numbers.RangedDouble
import org.bukkit.entity.Player
import org.gitee.orryx.utils.job
import taboolib.common.platform.Ghost
import taboolib.module.kether.orNull

@Ghost
class MythicMobsExperienceCondition(line: String, mlc: MythicLineConfig): SkillCondition(line), IEntityCondition {

    val exp: PlaceholderString = mlc.getPlaceholderString(arrayOf("exp", "e"), conditionVar, *arrayOfNulls<String>(0))

    override fun check(e: AbstractEntity): Boolean {
        return if (e.isPlayer) {
            val range = RangedDouble(exp.get(e))
            val job = (e.bukkitEntity as Player).job().orNull() ?: return false
            range.equals(job.experience)
        } else {
            false
        }
    }
}
