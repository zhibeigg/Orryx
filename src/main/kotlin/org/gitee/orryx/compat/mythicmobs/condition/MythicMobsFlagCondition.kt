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
class MythicMobsFlagCondition(line: String, mlc: MythicLineConfig): SkillCondition(line), IEntityCondition {

    val flag: PlaceholderString? = mlc.getPlaceholderString("flag", null)
    val value: PlaceholderString? = mlc.getPlaceholderString("value", null)

    override fun check(e: AbstractEntity): Boolean {
        return if (e.isPlayer) {
            val profile = (e.bukkitEntity as Player).orryxProfile().orNull()
            value?.get(e) == profile?.getFlag(flag?.get(e) ?: return false)?.value
        } else {
            false
        }
    }
}