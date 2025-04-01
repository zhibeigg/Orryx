package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.player.skill.OrryxClearAllSkillLevelAndBackPointEvent
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.job.PlayerJob
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.dao.cache.MemoryCache
import java.util.concurrent.CompletableFuture

fun IPlayerJob.clearAllLevelAndBackPoint(): CompletableFuture<Boolean> {
    val event = OrryxClearAllSkillLevelAndBackPointEvent(player, this)
    return if (event.call()) {
        CompletableFuture.allOf(
            *job.skills.map {
                player.getSkill(job.key, it).thenCompose { skill ->
                    skill?.clearLevelAndBackPoint() ?: CompletableFuture.completedFuture(null)
                }
            }.toTypedArray()
        ).thenApply { true }
    } else {
        CompletableFuture.completedFuture(false)
    }
}

fun IPlayerJob.getBindSkills(): Map<IBindKey, CompletableFuture<IPlayerSkill?>?> {
    val map = bindKeyOfGroup[BindKeyLoaderManager.getGroup(group)] ?: return bindKeys().associateWith { null }
    return bindKeys().associateWith { map[it]?.let { skill -> player.getSkill(job.key, skill) } }
}

fun IPlayerJob.getSkills(): List<CompletableFuture<IPlayerSkill?>> {
    return job.skills.map {
        player.skill(it, true) { skill -> skill }
    }
}

fun <T> IPlayerJob.skills(func: (skills: List<IPlayerSkill>) -> T): CompletableFuture<T> {
    val skills = mutableListOf<IPlayerSkill>()
    val fSkills = getSkills()
    return CompletableFuture.allOf(
        *fSkills.map { s ->
            s.thenAccept { skill ->
                skill?.let { skills.add(it) }
            }
        }.toTypedArray()
    ).thenApply {
        func(skills)
    }
}

fun <T> IPlayerJob.bindSkills(func: (bindSkills: Map<IBindKey, IPlayerSkill?>) -> T): CompletableFuture<T> {
    val bindSkills = mutableMapOf<IBindKey, IPlayerSkill?>()
    val fBindSkills = getBindSkills()
    return CompletableFuture.allOf(*fBindSkills.map { (k, s) ->
        s?.thenApply {
            bindSkills[k] = it
        } ?: CompletableFuture.completedFuture(null)
    }.toTypedArray()).thenApply {
        func(bindSkills)
    }
}

fun <T> Player.job(function: (IPlayerJob) -> T): CompletableFuture<T?> {
    return job().thenApply {
        it?.let { it1 -> function(it1) }
    }
}

fun <T> Player.job(job: String, function: (IPlayerJob) -> T): CompletableFuture<T?> {
    return job(job).thenApply {
        it?.let { it1 -> function(it1) }
    }
}

fun Player.job(): CompletableFuture<IPlayerJob?> {
    val future = CompletableFuture<IPlayerJob?>()
    orryxProfile { profile ->
        profile.job?.let {
            job(it) { job ->
                future.complete(job)
            }
        }
    }
    return future
}

fun Player.job(job: String): CompletableFuture<IPlayerJob?> {
    return MemoryCache.getPlayerJob(this, job).thenApply {
        it ?: defaultJob(job).apply {
            save()
        }
    }
}

private fun Player.defaultJob(job: String) = PlayerJob(this, job, 0, DEFAULT, hashMapOf())
