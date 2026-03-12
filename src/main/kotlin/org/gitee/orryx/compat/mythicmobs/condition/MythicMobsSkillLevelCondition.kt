package org.gitee.orryx.compat.mythicmobs.condition

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillCondition
import io.lumine.xikage.mythicmobs.skills.conditions.IEntityCondition
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderString
import io.lumine.xikage.mythicmobs.utils.numbers.RangedDouble
import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import taboolib.common.platform.Ghost
import taboolib.module.kether.orNull

@Ghost
class MythicMobsSkillLevelCondition(line: String, mlc: MythicLineConfig): SkillCondition(line), IEntityCondition {

    val skill: PlaceholderString = mlc.getPlaceholderString(arrayOf("skill", "s"), null)
    val level: PlaceholderString = mlc.getPlaceholderString(arrayOf("level", "l"), conditionVar, *arrayOfNulls<String>(0))

    override fun check(e: AbstractEntity): Boolean {
        return if (e.isPlayer) {
            val range = RangedDouble(level.get(e))
            val skillName = skill.get(e) ?: return false
            val skillLevel = Orryx.api().skillAPI.modifySkill(e.bukkitEntity as Player, skillName) { it.level }.orNull() ?: return false
            range.equals(skillLevel)
        } else {
            false
        }
    }
}
