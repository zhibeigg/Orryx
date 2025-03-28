package org.gitee.orryx.core.ui

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.skill.SkillLevelResult
import org.gitee.orryx.utils.*
import java.util.concurrent.CompletableFuture

abstract class AbstractSkillUI(override val viewer: Player, override val owner: Player): ISkillUI {

    override fun clearAndBackPoint(skill: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        owner.skill(skill) {
            it.clearLevelAndBackPoint().thenApply { bool ->
                future.complete(bool)
            }
        }
        return future
    }

    override fun upgrade(skill: String): CompletableFuture<SkillLevelResult> {
        val future = CompletableFuture<SkillLevelResult>()
        owner.getSkill(skill).thenApply {
            it?.up()?.thenApply {
                future.complete(it)
            } ?: future.complete(SkillLevelResult.NONE)
        }
        return future
    }

    override fun clearAllAndBackPoint(): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        owner.job { job ->
            job.clearAllLevelAndBackPoint().thenApply {
                future.complete(it)
            }
        }
        return future
    }

    override fun unBindSkill(job: IPlayerJob, skill: String, group: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        owner.skill(skill) { s ->
            BindKeyLoaderManager.getGroup(group)?.let { group ->
                job.unBindKey(s, group).thenApply {
                    future.complete(it)
                }
            } ?: future.complete(false)
        }
        return future
    }

    override fun bindSkill(job: IPlayerJob, skill: String, group: String, bindKey: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        owner.skill(skill) { s ->
            BindKeyLoaderManager.getGroup(group)?.let { group ->
                BindKeyLoaderManager.getBindKey(bindKey)?.let { bindKey ->
                    job.setBindKey(s, group, bindKey).thenApply {
                        future.complete(it)
                    }
                } ?: future.complete(false)
            }
        }
        return future
    }

}