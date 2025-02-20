package org.gitee.orryx.core.ui

import org.bukkit.entity.Player
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.profile.PlayerProfileManager.job
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.core.skill.SkillLevelResult
import org.gitee.orryx.utils.*

abstract class AbstractSkillUI(override val viewer: Player, override val owner: Player): ISkillUI {

    override fun getSkills(): List<IPlayerSkill> {
        return owner.job { job ->
            job.job.skills.mapNotNull {
                owner.getSkill(job.key, it)
            }
        } ?: emptyList()
    }

    override fun clearAndBackPoint(skill: String): Boolean {
        return owner.skill(skill) {
            it.clearLevelAndBackPoint()
        } ?: false
    }

    override fun getGroupSkills(group: String): Map<IBindKey, String?> {
        return owner.job { job ->
            BindKeyLoaderManager.getGroup(group)?.let { group ->
                job.bindKeyOfGroup[group]
            }
        } ?: emptyMap()
    }

    override fun upgrade(skill: String): SkillLevelResult {
        return owner.getSkill(skill)?.up() ?: SkillLevelResult.NONE
    }

    override fun clearAllAndBackPoint(): Boolean {
        return owner.job { job ->
            job.clearAllLevelAndBackPoint()
        } ?: false
    }

    override fun unBindSkill(skill: String, group: String): Boolean {
        return owner.job { job ->
            owner.skill(skill) { skill ->
                BindKeyLoaderManager.getGroup(group)?.let { group ->
                    job.unBindKey(skill, group)
                }
            }
        } ?: false
    }

    override fun bindSkill(skill: String, group: String, bindKey: String): Boolean {
        return owner.job { job ->
            owner.skill(skill) { skill ->
                BindKeyLoaderManager.getGroup(group)?.let { group ->
                    BindKeyLoaderManager.getBindKey(bindKey)?.let { bindKey ->
                        job.setBindKey(skill, group, bindKey)
                    }
                }
            }
        } ?: false
    }

}