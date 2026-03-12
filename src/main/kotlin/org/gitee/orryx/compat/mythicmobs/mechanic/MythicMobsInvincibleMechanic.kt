package org.gitee.orryx.compat.mythicmobs.mechanic

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.ITargetedEntitySkill
import io.lumine.xikage.mythicmobs.skills.SkillMechanic
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import org.gitee.orryx.api.Orryx
import taboolib.common.platform.Ghost

@Ghost
class MythicMobsInvincibleMechanic(line: String, mlc: MythicLineConfig) : SkillMechanic(line, mlc), ITargetedEntitySkill {

    init {
        isAsyncSafe = true
    }

    private val duration = mlc.getPlaceholderInteger(arrayOf("duration", "d"), 1000)

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity): Boolean {
        return if (target.isPlayer) {
            val player = BukkitAdapter.adapt(target.asPlayer()) ?: return false
            Orryx.api().profileAPI.setInvincible(player, duration.get(data).toLong())
            true
        } else {
            false
        }
    }
}
