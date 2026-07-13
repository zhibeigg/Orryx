package org.gitee.orryx.module.ui

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.skill.SkillLevelResult
import org.gitee.orryx.utils.*
import java.util.concurrent.CompletableFuture

abstract class AbstractSkillUI(override val viewer: Player, override val owner: Player): ISkillUI {

    override fun clearAndBackPoint(skill: String): CompletableFuture<Boolean> {
        return owner.getSkill(skill).thenComposeMain { playerSkill ->
            playerSkill?.clearLevelAndBackPoint() ?: CompletableFuture.completedFuture(false)
        }
    }

    override fun upgrade(skill: String): CompletableFuture<SkillLevelResult> {
        return owner.getSkill(skill).thenComposeMain { playerSkill ->
            playerSkill?.up() ?: CompletableFuture.completedFuture(SkillLevelResult.NONE)
        }
    }

    override fun clearAllAndBackPoint(): CompletableFuture<Boolean> {
        return owner.job().thenComposeMain { job ->
            job?.clearAllLevelAndBackPoint() ?: CompletableFuture.completedFuture(false)
        }
    }

    override fun unBindSkill(job: IPlayerJob, skill: String, group: String): CompletableFuture<Boolean> {
        val bindGroup = BindKeyLoaderManager.getGroup(group) ?: return CompletableFuture.completedFuture(false)
        return owner.getSkill(skill).thenComposeMain { playerSkill ->
            playerSkill?.let { job.unBindKey(it, bindGroup) } ?: CompletableFuture.completedFuture(false)
        }
    }

    override fun bindSkill(job: IPlayerJob, skill: String, group: String, bindKey: String): CompletableFuture<Boolean> {
        val bindGroup = BindKeyLoaderManager.getGroup(group) ?: return CompletableFuture.completedFuture(false)
        val key = BindKeyLoaderManager.getBindKey(bindKey) ?: return CompletableFuture.completedFuture(false)
        return owner.getSkill(skill).thenComposeMain { playerSkill ->
            playerSkill?.let { job.setBindKey(it, bindGroup, key) } ?: CompletableFuture.completedFuture(false)
        }
    }
}
