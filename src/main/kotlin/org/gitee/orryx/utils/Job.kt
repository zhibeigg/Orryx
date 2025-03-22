package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.player.skill.OrryxClearAllSkillLevelAndBackPointEvent
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.job.PlayerJob
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.profile.PlayerProfileManager.orryxProfile
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.dao.cache.ICacheManager
import taboolib.common.platform.function.isPrimaryThread
import java.util.concurrent.CompletableFuture

fun IPlayerJob.clearAllLevelAndBackPoint(): CompletableFuture<Boolean> {
    val event = OrryxClearAllSkillLevelAndBackPointEvent(player, this)
    val future = CompletableFuture<Boolean>()
    if (event.call()) {
        var size = 0
        job.skills.forEach {
            size++
            player.getSkill(job.key, it)?.clearLevelAndBackPoint()?.whenComplete { _, _ ->
                size -= 1
                if (size == 0) {
                    future.complete(true)
                }
            }
        }
    } else {
        future.complete(false)
    }
    return future
}

fun IPlayerJob.getBindSkills(): Map<IBindKey, IPlayerSkill?> {
    val map = bindKeyOfGroup[BindKeyLoaderManager.getGroup(group)] ?: return bindKeys().associateWith { null }
    return bindKeys().associateWith { map[it]?.let { skill -> player.getSkill(job.key, skill) } }
}

fun <T> Player.job(function: (IPlayerJob) -> T): T? {
    return job()?.let {
        function(it)
    }
}

fun Player.job(): IPlayerJob? {
    val job = orryxProfile().job ?: return null
    return job(job)
}

fun Player.job(job: String): IPlayerJob {
    return ICacheManager.INSTANCE.getPlayerJob(uniqueId, job)?.let {
        PlayerJob(this, it.job, it.experience, it.group, bindKeyOfGroupToMutableMap(it.bindKeyOfGroup))
    } ?: defaultJob(job).apply {
        save(isPrimaryThread)
    }
}

private fun Player.defaultJob(job: String) = PlayerJob(this, job, 0, DEFAULT, hashMapOf())
