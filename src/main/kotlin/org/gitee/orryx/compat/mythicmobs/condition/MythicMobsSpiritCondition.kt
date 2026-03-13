package org.gitee.orryx.compat.mythicmobs.condition

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillCondition
import io.lumine.xikage.mythicmobs.skills.conditions.IEntityCondition
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderString
import io.lumine.xikage.mythicmobs.utils.numbers.RangedDouble
import org.bukkit.entity.Player
import org.gitee.orryx.module.spirit.ISpiritManager
import taboolib.common.platform.Ghost

@Ghost
class MythicMobsSpiritCondition(line: String, mlc: MythicLineConfig): SkillCondition(line), IEntityCondition {

    val spirit: PlaceholderString = mlc.getPlaceholderString(arrayOf("spirit", "s"), conditionVar, *arrayOfNulls<String>(0))

    override fun check(e: AbstractEntity): Boolean {
        return if (e.isPlayer) {
            val range = RangedDouble(spirit.get(e))
            val spirit = ISpiritManager.INSTANCE.getSpirit(e.bukkitEntity as Player)
            range.equals(spirit)
        } else {
            false
        }
    }
}
