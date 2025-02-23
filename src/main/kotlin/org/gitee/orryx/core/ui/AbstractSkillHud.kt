package org.gitee.orryx.core.ui

import org.bukkit.entity.Player
import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.utils.job

abstract class AbstractSkillHud(override val viewer: Player, override val owner: Player): ISkillHud {

    override fun getShowSkills(): Map<IBindKey, String?> {
        return owner.job {
            it.bindKeyOfGroup[BindKeyLoaderManager.getGroup(it.group)]
        } ?: emptyMap()
    }

    override fun getCountdown(skill: IPlayerSkill): Long {
        return SkillTimer.getCountdown(skill.player, skill.key)
    }

    override fun setGroup(group: IGroup): Boolean {
        return owner.job {
            it.setGroup(group.key)
        } ?: false
    }

}