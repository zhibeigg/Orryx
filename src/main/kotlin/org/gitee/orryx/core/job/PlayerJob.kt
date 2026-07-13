package org.gitee.orryx.core.job

import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.OrryxPlayerChangeGroupEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobClearEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobExperienceEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobLevelEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobSaveEvents
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillBindKeyEvent
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillClearEvents
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillUnBindKeyEvent
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.job.ExperienceResult.*
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.ICastSkill
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.core.skill.PlayerSkill
import org.gitee.orryx.dao.cache.ISyncCacheManager
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.persistence.PersistenceManager
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.module.experience.ExperienceLoaderManager
import org.gitee.orryx.module.experience.IExperience
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.module.kether.orNull
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class PlayerJob(
    override val id: Int,
    override val uuid: UUID,
    override val key: String,
    private var privateExperience: Int,
    private var privateGroup: String = DEFAULT,
    private val privateBindKeyOfGroup: MutableMap<IGroup, MutableMap<IBindKey, String?>>
): IPlayerJob {

    constructor(id: Int, player: Player, key: String, privateExperience: Int, privateGroup: String = DEFAULT, privateBindKeyOfGroup: MutableMap<IGroup, MutableMap<IBindKey, String?>>): this(id, player.uniqueId, key, privateExperience, privateGroup, privateBindKeyOfGroup)

    override val player
        get() = Bukkit.getPlayer(uuid) ?: error("Player Offline")

    override val job: IJob
        get() = JobLoaderManager.getJobLoader(key)!!

    override val bindKeyOfGroup: Map<IGroup, Map<IBindKey, String?>>
        get() = privateBindKeyOfGroup

    override val experience: Int
        get() = privateExperience

    override val group: String
        get() = privateGroup

    override val experienceOfLevel: Int
        get() = getExperience().getLessExp(player, experience)

    override val level: Int
        get() = getExperience().getLevel(player, experience)

    override val maxLevel: Int
        get() = getExperience().maxLevel

    override val maxExperienceOfLevel: Int
        get() = getExperience().getExperienceOfLevel(player, level)

    override fun createPO(): PlayerJobPO {
        return PlayerJobPO(id, uuid, key, experience, group, bindKeyOfGroupToMap(bindKeyOfGroup))
    }

    override fun getUpgradePoint(from: Int, to: Int): Int {
        return player.eval(job.upgradePointActions, mapOf("from" to from, "to" to to)).orNull().cint
    }

    override fun getAttributes(): List<String> {
        return player.parse(job.attributes, mapOf("level" to level))
    }

    override fun getExperience(): IExperience {
        return ExperienceLoaderManager.getExperience(job.experience) ?: error("职业${key}的经验计算器${job.experience}找不到")
    }

    override fun getMaxMana(): Double {
        check(isPrimaryThread) { "同步 getMaxMana 必须在 Bukkit 主线程调用" }
        return completedValue(getMaxManaAsync(), "MaxMana")
    }

    override fun getMaxManaAsync(): CompletableFuture<Double> {
        return player.eval(job.maxManaActions, mapOf("level" to level)).thenApply { it.cdouble }
    }

    override fun getRegainMana(): Double {
        check(isPrimaryThread) { "同步 getRegainMana 必须在 Bukkit 主线程调用" }
        return completedValue(getRegainManaAsync(), "RegainMana")
    }

    override fun getRegainManaAsync(): CompletableFuture<Double> {
        return player.eval(job.regainManaActions, mapOf("level" to level)).thenApply { it.cdouble }
    }

    override fun getMaxSpirit(): Double {
        check(isPrimaryThread) { "同步 getMaxSpirit 必须在 Bukkit 主线程调用" }
        return completedValue(getMaxSpiritAsync(), "MaxSpirit")
    }

    override fun getMaxSpiritAsync(): CompletableFuture<Double> {
        return player.eval(job.maxSpiritActions, mapOf("level" to level)).thenApply { it.cdouble }
    }

    override fun getRegainSpirit(): Double {
        check(isPrimaryThread) { "同步 getRegainSpirit 必须在 Bukkit 主线程调用" }
        return completedValue(getRegainSpiritAsync(), "RegainSpirit")
    }

    override fun getRegainSpiritAsync(): CompletableFuture<Double> {
        return player.eval(job.regainSpiritActions, mapOf("level" to level)).thenApply { it.cdouble }
    }

    private fun completedValue(future: CompletableFuture<Double>, name: String): Double {
        check(future.isDone) { "职业 $key 的 $name 不允许包含异步动作，请使用对应 Future API" }
        return try {
            future.getNow(0.0)
        } catch (throwable: CompletionException) {
            throw throwable.cause ?: throwable
        }
    }

    override fun giveExperience(experience: Int): CompletableFuture<ExperienceResult> {
        if (experience < 0) return takeExperience(-experience)
        val event = OrryxPlayerJobExperienceEvents.Up.Pre(player, this, experience)
        val future = CompletableFuture<ExperienceResult>()
        if (event.call()) {
            val before = level
            privateExperience = (privateExperience + event.upExperience.coerceAtLeast(0)).coerceAtMost(getExperience().maxExp(player))
            persist(isPrimaryThread, false).whenComplete { _, throwable ->
                runOnMainThread {
                    if (throwable != null) {
                        future.completeExceptionally(throwable)
                    } else {
                        OrryxPlayerJobExperienceEvents.Up.Post(player, this, event.upExperience).call()
                        val changeLevel = level - before
                        if (changeLevel > 0) {
                            val levelEvent = OrryxPlayerJobLevelEvents.Up.Pre(player, this, changeLevel)
                            if (levelEvent.call()) {
                                OrryxPlayerJobLevelEvents.Up.Post(player, this, levelEvent.upLevel).call()
                            }
                        }
                        future.complete(SUCCESS)
                    }
                }
            }
        } else {
            future.complete(CANCELLED)
        }
        return future
    }

    override fun takeExperience(experience: Int): CompletableFuture<ExperienceResult> {
        if (experience < 0) return giveExperience(-experience)
        val event = OrryxPlayerJobExperienceEvents.Down.Pre(player, this, experience)
        val future = CompletableFuture<ExperienceResult>()
        if (event.call()) {
            val before = level
            privateExperience = (privateExperience - event.downExperience.coerceAtLeast(0)).coerceAtLeast(0)
            persist(isPrimaryThread, false).whenComplete { _, throwable ->
                runOnMainThread {
                    if (throwable != null) {
                        future.completeExceptionally(throwable)
                    } else {
                        OrryxPlayerJobExperienceEvents.Down.Post(player, this, event.downExperience).call()
                        val changeLevel = before - level
                        if (changeLevel > 0) {
                            val levelEvent = OrryxPlayerJobLevelEvents.Down.Pre(player, this, changeLevel)
                            if (levelEvent.call()) {
                                OrryxPlayerJobLevelEvents.Down.Post(player, this, levelEvent.downLevel).call()
                            }
                        }
                        future.complete(SUCCESS)
                    }
                }
            }
        } else {
            future.complete(CANCELLED)
        }
        return future
    }

    override fun setExperience(experience: Int): CompletableFuture<ExperienceResult> {
        return when {
            experience > this.experience -> giveExperience(experience - this.experience)
            experience < this.experience -> takeExperience(this.experience - experience)
            else -> CompletableFuture.completedFuture(SAME)
        }
    }

    override fun giveLevel(level: Int): CompletableFuture<LevelResult> {
        if (level < 0) return takeLevel(-level)
        return giveExperience(getExperience().getExperienceFromTo(player, this.level, this.level + level)).thenApply { result ->
            when(result) {
                CANCELLED -> LevelResult.CANCELLED
                SUCCESS -> LevelResult.SUCCESS
                SAME -> LevelResult.SAME
                else -> error("Give Level Result NULL")
            }
        }
    }

    override fun takeLevel(level: Int): CompletableFuture<LevelResult> {
        if (level < 0) return giveLevel(-level)
        return takeExperience(getExperience().getExperienceFromTo(player, this.level - level, this.level)).thenApply { result ->
            when(result) {
                CANCELLED -> LevelResult.CANCELLED
                SUCCESS -> LevelResult.SUCCESS
                SAME -> LevelResult.SAME
                else -> error("Take Level Result NULL")
            }
        }
    }

    override fun setLevel(level: Int): CompletableFuture<LevelResult> {
        return when {
            level < this.level -> takeLevel(this.level - level)
            level > this.level -> giveLevel(level - this.level)
            else -> CompletableFuture.completedFuture(LevelResult.SAME)
        }
    }

    override fun setGroup(group: String): CompletableFuture<Boolean> {
        val iGroup = BindKeyLoaderManager.getGroup(group) ?: return CompletableFuture.completedFuture(false)
        val event = OrryxPlayerChangeGroupEvents.Pre(player, this, iGroup)
        val future = CompletableFuture<Boolean>()
        if (event.call()) {
            privateGroup = event.group.key
            persist(isPrimaryThread, false).whenComplete { _, throwable ->
                runOnMainThread {
                    if (throwable != null) future.completeExceptionally(throwable) else {
                        OrryxPlayerChangeGroupEvents.Post(player, this, event.group).call()
                        future.complete(true)
                    }
                }
            }
        } else {
            future.complete(false)
        }
        return future
    }

    override fun setBindKey(skill: IPlayerSkill, group: IGroup, bindKey: IBindKey): CompletableFuture<Boolean> {
        if (skill.skill !is ICastSkill) return CompletableFuture.completedFuture(false)
        val event = OrryxPlayerSkillBindKeyEvent.Pre(player, skill, group, bindKey)
        val future = CompletableFuture<Boolean>()
        if (event.call()) {
            privateBindKeyOfGroup.getOrPut(event.group) { hashMapOf() }.apply {
                replaceAll { _, u ->
                    if (u == skill.key) {
                        null
                    } else {
                        u
                    }
                }
                set(event.bindKey, skill.key)
            }
            persist(isPrimaryThread, false).whenComplete { _, throwable ->
                runOnMainThread {
                    if (throwable != null) future.completeExceptionally(throwable) else {
                        OrryxPlayerSkillBindKeyEvent.Post(player, skill, event.group, event.bindKey).call()
                        future.complete(true)
                    }
                }
            }
        } else {
            future.complete(false)
        }
        return future
    }

    override fun unBindKey(skill: IPlayerSkill, group: IGroup): CompletableFuture<Boolean> {
        val event = OrryxPlayerSkillUnBindKeyEvent.Pre(player, skill, group)
        val future = CompletableFuture<Boolean>()
        if (event.call()) {
            privateBindKeyOfGroup[event.group]?.replaceAll { _, u ->
                if (u == skill.key) {
                    null
                } else {
                    u
                }
            }
            persist(isPrimaryThread, false).whenComplete { _, throwable ->
                runOnMainThread {
                    if (throwable != null) future.completeExceptionally(throwable) else {
                        OrryxPlayerSkillUnBindKeyEvent.Post(player, skill, event.group).call()
                        future.complete(true)
                    }
                }
            }
        } else {
            future.complete(false)
        }
        return future
    }

    override fun clear(): CompletableFuture<Boolean> {
        return mainThreadFuture {
            val onlinePlayer = player
            if (!OrryxPlayerJobClearEvents.Pre(onlinePlayer, this).call()) {
                return@mainThreadFuture null
            }
            onlinePlayer to job.skills.map { skillKey -> onlinePlayer.getSkill(key, skillKey) }
        }.thenCompose { preparation ->
            if (preparation == null) {
                CompletableFuture.completedFuture(null)
            } else {
                val (onlinePlayer, skillFutures) = preparation
                CompletableFuture.allOf(*skillFutures.toTypedArray()).thenApplyMain {
                    privateBindKeyOfGroup.clear()
                    privateExperience = 0
                    privateGroup = DEFAULT
                    val clearedSkills = skillFutures.mapNotNull { it.getNow(null) }
                        .filterIsInstance<PlayerSkill>()
                        .filter { it.clearMemoryState() }
                    ClearContext(onlinePlayer, clearedSkills, createPO(), clearedSkills.map { it.createPO() })
                }
            }
        }.thenCompose { context ->
            if (context == null) {
                CompletableFuture.completedFuture(null)
            } else {
                PersistenceManager.saveJobAndSkills(context.job, context.skillData, invalidate = true)
                    .thenApply { context }
            }
        }.thenApplyMain { context ->
            if (context == null) {
                false
            } else {
                context.skills.forEach { skill ->
                    OrryxPlayerSkillClearEvents.Post(context.player, skill).call()
                }
                OrryxPlayerJobClearEvents.Post(context.player, this).call()
                true
            }
        }
    }

    override fun save(async: Boolean, remove: Boolean, callback: Runnable) {
        persist(async, remove).whenComplete { context, throwable ->
            finishSaveCallback(callback, throwable) {
                OrryxPlayerJobSaveEvents.Post(context.player, this@PlayerJob, context.async, context.remove).call()
            }
        }
    }

    private fun persist(async: Boolean, remove: Boolean): CompletableFuture<SaveContext> {
        return mainThreadFuture {
            val onlinePlayer = player
            val event = OrryxPlayerJobSaveEvents.Pre(onlinePlayer, this, async, remove)
            event.call()
            SaveContext(onlinePlayer, event.async, event.remove, createPO())
        }.thenCompose { context ->
            PersistenceManager.saveJob(context.data, context.remove).thenApply { context }
        }
    }

    private data class SaveContext(
        val player: Player,
        val async: Boolean,
        val remove: Boolean,
        val data: PlayerJobPO,
    )

    private data class ClearContext(
        val player: Player,
        val skills: List<PlayerSkill>,
        val job: PlayerJobPO,
        val skillData: List<org.gitee.orryx.dao.pojo.PlayerSkillPO>,
    )

    override fun toString(): String {
        return "PlayerJob(player=${player.name}, key=$key, level=$level)"
    }
}
