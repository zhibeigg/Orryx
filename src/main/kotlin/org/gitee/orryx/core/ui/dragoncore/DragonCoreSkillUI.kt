package org.gitee.orryx.core.ui.dragoncore

import eos.moe.dragoncore.network.PacketSender
import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.PlayerProfileManager.orryxProfile
import org.gitee.orryx.core.ui.AbstractSkillUI
import org.gitee.orryx.utils.getBindSkills
import org.gitee.orryx.utils.getSkills
import org.gitee.orryx.utils.job

class DragonCoreSkillUI(override val viewer: Player, override val owner: Player): AbstractSkillUI(viewer, owner) {

    companion object {

        private fun update(viewer: Player, owner: Player) {
            val job = owner.job() ?: return
            val bindSkills = job.getBindSkills()
            val keys = bindSkills.keys.sortedBy { it.sort }
            val skills = owner.getSkills()

            PacketSender.sendSyncPlaceholder(viewer, mapOf(
                "Orryx_job" to job.job.name,
                "Orryx_point" to owner.orryxProfile().point.toString(),
                "Orryx_group" to job.group,
                "Orryx_bind_keys" to keys.joinToString("<br>") { it.key },
                "Orryx_bind_skills" to keys.joinToString("<br>") { it.key },
                "Orryx_skills" to skills.joinToString("<br>") { it.key },
                "Orryx_skills_level" to skills.joinToString("<br>") { it.level.toString() },
                "Orryx_skills_maxLevel" to skills.joinToString("<br>") { it.skill.maxLevel.toString() },
                "Orryx_skills_locked" to skills.joinToString("<br>") { it.locked.toString() },
            ))
        }

    }

    override fun open() {
        PacketSender.sendOpenGui(viewer, "OrryxSkillUI")
    }

    override fun update() {
        TODO("Not yet implemented")
    }

}