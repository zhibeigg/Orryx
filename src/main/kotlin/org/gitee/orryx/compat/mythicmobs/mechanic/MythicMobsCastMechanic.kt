package org.gitee.orryx.compat.mythicmobs.mechanic

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.ITargetedEntitySkill
import io.lumine.xikage.mythicmobs.skills.SkillMechanic
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.skill.ICastSkill
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.castSkill
import taboolib.common.platform.Ghost

@Ghost
class MythicMobsCastMechanic(line: String, mlc: MythicLineConfig) : SkillMechanic(line, mlc), ITargetedEntitySkill {

    init {
        isAsyncSafe = true
    }

    private val skill = mlc.getPlaceholderString(arrayOf("s", "skill"), null)
    private val level = mlc.getPlaceholderInteger(arrayOf("l", "level"), 1)

    override fun castAtEntity(data: SkillMetadata, target: AbstractEntity): Boolean {
        return if (target.isPlayer) {
            val player = BukkitAdapter.adapt(target.asPlayer()) ?: return false
            val skill = SkillLoaderManager.getSkillLoader(skill.get(data)) as ICastSkill
            skill.castSkill(player, SkillParameter(skill.key, player, level.get()), false)
            true
        } else {
            false
        }
    }
}