package org.gitee.orryx.core.ui

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.skill.SkillLevelResult
import org.gitee.orryx.utils.*
import java.util.concurrent.CompletableFuture

abstract class AbstractSkillUI(override val viewer: Player, override val owner: Player): ISkillUI {

    override fun clearAndBackPoint(skill: String): CompletableFuture<Boolean> {
        return owner.skill(skill) {
            it.clearLevelAndBackPoint()
        } ?: CompletableFuture.completedFuture(false)
    }

    override fun upgrade(skill: String): CompletableFuture<SkillLevelResult> {
        return owner.getSkill(skill)?.up() ?: CompletableFuture.completedFuture(SkillLevelResult.NONE)
    }

    override fun clearAllAndBackPoint(): CompletableFuture<Boolean> {
        return owner.job { job ->
            job.clearAllLevelAndBackPoint()
        } ?: CompletableFuture.completedFuture(false)
    }

    override fun unBindSkill(job: IPlayerJob, skill: String, group: String): CompletableFuture<Boolean> {
        return owner.skill(skill) { s ->
            BindKeyLoaderManager.getGroup(group)?.let { group ->
                job.unBindKey(s, group)
            }
        } ?: CompletableFuture.completedFuture(false)
    }

    override fun bindSkill(job: IPlayerJob, skill: String, group: String, bindKey: String): CompletableFuture<Boolean> {
        return owner.skill(skill) { s ->
            BindKeyLoaderManager.getGroup(group)?.let { group ->
                BindKeyLoaderManager.getBindKey(bindKey)?.let { bindKey ->
                    job.setBindKey(s, group, bindKey)
                }
            }
        } ?: CompletableFuture.completedFuture(false)
    }

}