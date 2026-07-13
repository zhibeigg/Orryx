package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.player.skill.OrryxClearAllSkillLevelAndBackPointEvent
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.job.PlayerJob
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.persistence.PersistenceManager
import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

private val pendingJobCreations = ConcurrentHashMap<String, CompletableFuture<IPlayerJob?>>()

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
    val sourceMap = bindKeyOfGroup[BindKeyLoaderManager.getGroup(group)] ?: return bindKeys().associateWith { null }
    val map = bindKeys().associateWith { sourceMap[it] }
    return map.mapValues { it.value?.let { skill -> player.getSkill(key, skill, true) } }
}

fun IPlayerJob.getSkills(): List<CompletableFuture<IPlayerSkill?>> {
    return job.skills.map {
        player.skill(key, it, true) { skill -> skill }
    }
}

inline fun <T> IPlayerJob.skills(crossinline func: (skills: List<IPlayerSkill>) -> T): CompletableFuture<T> {
    val skills = Collections.synchronizedList(mutableListOf<IPlayerSkill>())
    val fSkills = getSkills()
    return CompletableFuture.allOf(
        *fSkills.map { s ->
            s.thenAccept { skill ->
                skill?.let { skills.add(it) }
            }
        }.toTypedArray()
    ).thenApplyMain {
        func(skills.sortedBy { it.skill.sort })
    }
}

inline fun <T> IPlayerJob.bindSkills(crossinline func: (bindSkills: Map<IBindKey, IPlayerSkill?>) -> T): CompletableFuture<T> {
    val bindSkills = ConcurrentHashMap<IBindKey, IPlayerSkill>()
    return CompletableFuture.allOf(*getBindSkills().map { (k, s) ->
        s?.thenAccept {
            if (it != null) {
                bindSkills[k] = it
            }
        } ?: CompletableFuture.completedFuture(null)
    }.toTypedArray()).thenApplyMain {
        func(bindSkills)
    }
}

inline fun <T> Player.job(crossinline function: (IPlayerJob) -> T): CompletableFuture<T?> {
    return job().thenApplyMain {
        it?.let { it1 -> function(it1) }
    }
}

inline fun <T> Player.job(id: Int, job: String, crossinline function: (IPlayerJob) -> T): CompletableFuture<T?> {
    return job(id, job).thenApplyMain {
        it?.let { it1 -> function(it1) }
    }
}

fun Player.job(): CompletableFuture<IPlayerJob?> {
    return orryxProfile { profile ->
        profile.job?.let {
            job(profile.id, it)
        } ?: CompletableFuture.completedFuture(null)
    }
}

fun Player.job(id: Int, job: String): CompletableFuture<IPlayerJob?> {
    val tag = playerJobDataTag(uniqueId, id, job)
    return MemoryCache.getPlayerJob(uniqueId, id, job).thenComposeMain { cached ->
        if (cached != null) return@thenComposeMain CompletableFuture.completedFuture(cached)
        val creation = pendingJobCreations.computeIfAbsent(tag) {
            val created = defaultJob(id, job)
            MemoryCache.savePlayerJob(created)
            PersistenceManager.saveJob(created.createPO(), invalidate = false).thenApply<IPlayerJob?> {
                created
            }.whenComplete { _, throwable ->
                if (throwable != null) MemoryCache.removePlayerJob(uniqueId, id, job)
            }
        }
        creation.whenComplete { _, _ -> pendingJobCreations.remove(tag, creation) }
        creation
    }
}

private fun Player.defaultJob(id: Int, job: String) = PlayerJob(id, this, job, 0, DEFAULT, hashMapOf())
