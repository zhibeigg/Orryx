package org.gitee.orryx.compat.mythicmobs.condition

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillCondition
import io.lumine.xikage.mythicmobs.skills.conditions.IEntityCondition
import io.lumine.xikage.mythicmobs.utils.numbers.RangedDouble
import org.bukkit.entity.Player
import org.gitee.orryx.module.mana.IManaManager
import taboolib.common.platform.Ghost
import taboolib.module.kether.orNull

@Ghost
class MythicMobsManaCondition(line: String, mlc: MythicLineConfig): SkillCondition(line), IEntityCondition {

    val mana: RangedDouble = RangedDouble(mlc.getString(arrayOf("mana", "m"), conditionVar, *arrayOfNulls<String>(0)))

    override fun check(e: AbstractEntity): Boolean {
        return if (e.isPlayer) {
            val mana = IManaManager.INSTANCE.getMana(e.bukkitEntity as Player).orNull()
            this.mana.equals(mana)
        } else {
            false
        }
    }
}