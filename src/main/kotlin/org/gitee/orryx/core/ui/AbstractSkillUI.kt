package org.gitee.orryx.core.ui

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.skill.SkillLevelResult
import org.gitee.orryx.utils.*

abstract class AbstractSkillUI(override val viewer: Player, override val owner: Player): ISkillUI {

    override fun clearAndBackPoint(skill: String): Boolean {
        return owner.skill(skill) {
            it.clearLevelAndBackPoint()
        } ?: false
    }

    override fun upgrade(skill: String): SkillLevelResult {
        return owner.getSkill(skill)?.up() ?: SkillLevelResult.NONE
    }

    override fun clearAllAndBackPoint(): Boolean {
        return owner.job { job ->
            job.clearAllLevelAndBackPoint()
        } ?: false
    }

    override fun unBindSkill(job: IPlayerJob, skill: String, group: String): Boolean {
        return owner.skill(skill) { s ->
            BindKeyLoaderManager.getGroup(group)?.let { group ->
                job.unBindKey(s, group)
            }
        } ?: false
    }

    override fun bindSkill(job: IPlayerJob, skill: String, group: String, bindKey: String): Boolean {
        return owner.skill(skill) { s ->
            BindKeyLoaderManager.getGroup(group)?.let { group ->
                BindKeyLoaderManager.getBindKey(bindKey)?.let { bindKey ->
                    job.setBindKey(s, group, bindKey)
                }
            }
        } ?: false
    }

}