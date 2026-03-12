package org.gitee.orryx.compat.mythicmobs.mechanic

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.ITargetedEntitySkill
import io.lumine.xikage.mythicmobs.skills.SkillMechanic
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import org.gitee.orryx.module.mana.IManaManager
import taboolib.common.platform.Ghost

@Ghost
class MythicMobsTakeManaMechanic(line: String, mlc: MythicLineConfig) : SkillMechanic(line, mlc), ITargetedEntitySkill {

    init {
        isAsyncSafe = true
    }

    private val amount = mlc.getPlaceholderDouble(arrayOf("amount", "a"), 0.0)

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity): Boolean {
        return if (target.isPlayer) {
            val player = BukkitAdapter.adapt(target.asPlayer()) ?: return false
            IManaManager.INSTANCE.takeMana(player, amount.get(data))
            true
        } else {
            false
        }
    }
}
