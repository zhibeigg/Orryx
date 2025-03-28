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
    val future = CompletableFuture<Boolean>()
    if (event.call()) {
        var size = 0
        job.skills.forEach {
            size++
            player.getSkill(job.key, it).thenApply { skill ->
                skill?.clearLevelAndBackPoint()?.whenComplete { _, _ ->
                    size -= 1
                    if (size == 0) {
                        future.complete(true)
                    }
                }
            }
        }
    } else {
        future.complete(false)
    }
    return future
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
    val future = CompletableFuture<T>()
    fSkills.forEach { s ->
        s.thenAccept { skill ->
            skills += skill!!
            if (skills.size >= fSkills.size) {
                future.complete(func(skills))
            }
        }
    }
    return future
}

fun <T> IPlayerJob.bindSkills(func: (bindSkills: Map<IBindKey, IPlayerSkill?>) -> T): CompletableFuture<T> {
    val bindSkills = mutableMapOf<IBindKey, IPlayerSkill?>()
    val fBindSkills = getBindSkills()
    val future = CompletableFuture<T>()
    fBindSkills.forEach { (k, s) ->
        s?.thenAccept { skill ->
            bindSkills[k] = skill
            if (bindSkills.size >= fBindSkills.size) {
                future.complete(func(bindSkills))
            }
        } ?: kotlin.run {
            bindSkills[k] = null
            if (bindSkills.size >= fBindSkills.size) {
                future.complete(func(bindSkills))
            }
        }
    }
    return future
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
