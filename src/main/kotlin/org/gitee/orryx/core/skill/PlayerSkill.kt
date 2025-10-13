package org.gitee.orryx.core.skill

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCastEvents
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillClearEvents
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillLevelEvents
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.skill.skills.PassiveSkill
import org.gitee.orryx.dao.cache.ISyncCacheManager
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.utils.async
import org.gitee.orryx.utils.castSkill
import org.gitee.orryx.utils.orryxProfile
import org.gitee.orryx.utils.orryxProfileTo
import org.gitee.orryx.utils.runCustomAction
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common5.cbool
import taboolib.common5.cint
import taboolib.module.kether.orNull
import java.util.UUID
import java.util.concurrent.CompletableFuture

class PlayerSkill(
    override val id: Int,
    override val uuid: UUID,
    override val key: String,
    override val job: String,
    private var privateLevel: Int,
    private var privateLocked: Boolean
): IPlayerSkill {

    constructor(id: Int, player: Player, key: String, job: String, privateLevel: Int, privateLocked: Boolean): this(id, player.uniqueId, key, job, privateLevel, privateLocked)

    override val player
        get() = Bukkit.getPlayer(uuid) ?: error("Player Offline")

    override val level: Int
        get() = privateLevel

    override val locked: Boolean
        get() = privateLocked

    override val skill: ISkill
        get() = SkillLoaderManager.getSkillLoader(key)!!

    override fun cast(parameter: IParameter, consume: Boolean): CastResult {
        if (parameter !is SkillParameter) return CastResult.PARAMETER
        return if (OrryxPlayerSkillCastEvents.Cast(player, this, parameter).call()) {
            skill.castSkill(player, parameter, consume)
            CastResult.SUCCESS
        } else {
            CastResult.CANCELED
        }
    }

    override fun castCheck(parameter: IParameter): CompletableFuture<CastResult> {
        if (parameter !is SkillParameter) return CompletableFuture.completedFuture(CastResult.PARAMETER)
        val skill = skill
        // 被动技能
        if (skill is PassiveSkill) return CompletableFuture.completedFuture(CastResult.PASSIVE)
        // 蓄力技能
        if (PressSkillManager.pressTaskMap.containsKey(player.uniqueId)) return CompletableFuture.completedFuture(CastResult.PRESSING)
        // 指向性技能
        if (PluginMessageHandler.pendingRequests.containsKey(player.uniqueId)) return CompletableFuture.completedFuture(CastResult.AIMING)
        // 沉默
        if (!skill.ignoreSilence && Orryx.api().profileAPI.isSilence(player)) return CompletableFuture.completedFuture(CastResult.SILENCE)
        // 冷却
        if (!SkillTimer.hasNext(player, key)) return CompletableFuture.completedFuture(CastResult.COOLDOWN)
        // 法力
        return IManaManager.INSTANCE.haveMana(player, parameter.manaValue()).thenApply { mana ->
            if (!mana) return@thenApply CastResult.MANA_NOT_ENOUGH
            // 脚本检测
            if ((skill as? ICastSkill)?.castCheckAction?.let { parameter.runCustomAction(it, mapOf()).orNull().cbool } == false) return@thenApply CastResult.CHECK_ACTION_FAILED
            // 事件
            if (!OrryxPlayerSkillCastEvents.Check(player, this, parameter).call()) return@thenApply CastResult.CANCELED
            return@thenApply CastResult.SUCCESS
        }
    }

    override fun upLevelCheck(from: Int, to: Int): Boolean {
        skill.upLevelCheckAction?.let {
            return runCustomAction(it, mapOf("from" to from, "to" to to)).orNull().cbool
        }
        return true
    }

    override fun upLevelSuccess(from: Int, to: Int) {
        skill.upLevelSuccessAction?.let {
            runCustomAction(it, mapOf("from" to from, "to" to to))
        }
    }

    override fun upgradePointCheck(from: Int, to: Int): CompletableFuture<Pair<Int,Boolean>> {
        val point = skill.upgradePointAction?.let { runCustomAction(it, mapOf("from" to from, "to" to to)).orNull() }.cint
        return player.orryxProfileTo {
            point to (it.point >= point)
        }
    }

    override fun upLevel(level: Int): CompletableFuture<SkillLevelResult> {
        if (level < 0) return downLevel(-level)
        val event = OrryxPlayerSkillLevelEvents.Up.Pre(player, this, level)
        val future = CompletableFuture<SkillLevelResult>()
        if (event.call()) {
            var result = SkillLevelResult.SUCCESS
            if (event.upLevel <= 0) error("升级等级必须>0")
            if (privateLevel + event.upLevel > skill.maxLevel) result = SkillLevelResult.MIN
            privateLevel = (privateLevel + event.upLevel).coerceAtMost(skill.maxLevel)
            save(isPrimaryThread) {
                OrryxPlayerSkillLevelEvents.Up.Post(player, this, event.upLevel)
                future.complete(result)
            }
        } else {
            future.complete(SkillLevelResult.CANCELLED)
        }
        return future
    }

    override fun downLevel(level: Int): CompletableFuture<SkillLevelResult> {
        if (level < 0) return upLevel(-level)
        val event = OrryxPlayerSkillLevelEvents.Down.Pre(player, this, level)
        val future = CompletableFuture<SkillLevelResult>()
        if (event.call()) {
            var result = SkillLevelResult.SUCCESS
            if (event.downLevel <= 0) error("降级等级必须>0")
            if (privateLevel - event.downLevel < skill.minLevel) result = SkillLevelResult.MIN
            privateLevel = (privateLevel - event.downLevel).coerceAtLeast(skill.minLevel)
            save(isPrimaryThread) {
                OrryxPlayerSkillLevelEvents.Down.Pre(player, this, event.downLevel).call()
                future.complete(result)
            }
        } else {
            future.complete(SkillLevelResult.CANCELLED)
        }
        return future
    }

    override fun setLevel(level: Int): CompletableFuture<SkillLevelResult> {
        return when {
            level > this.level -> upLevel(level - this.level)
            level < this.level -> downLevel(this.level - level)
            else -> CompletableFuture.completedFuture(SkillLevelResult.SAME)
        }
    }

    override fun createPO(): PlayerSkillPO {
        return PlayerSkillPO(id, player.uniqueId, job, key, locked, level)
    }

    override fun clear(): CompletableFuture<Boolean> {
        val event = OrryxPlayerSkillClearEvents.Pre(player, this)
        val future = CompletableFuture<Boolean>()
        if (event.call()) {
            privateLocked = skill.isLocked
            privateLevel = skill.minLevel
            save(isPrimaryThread) {
                OrryxPlayerSkillClearEvents.Post(player, this).call()
                future.complete(true)
            }
        } else {
            future.complete(false)
        }
        return future
    }

    override fun save(async: Boolean, remove: Boolean, callback: () -> Unit) {
        val data = createPO()
        fun remove() {
            if (remove) {
                ISyncCacheManager.INSTANCE.removePlayerSkill(player.uniqueId, id, job, key)
                MemoryCache.removePlayerSkill(player.uniqueId, id, job, key)
            }
        }
        if (async && !GameManager.shutdown) {
            OrryxAPI.saveScope.launch(Dispatchers.async) {
                IStorageManager.INSTANCE.savePlayerSkill(data) {
                    remove()
                    callback()
                }
            }
        } else {
            IStorageManager.INSTANCE.savePlayerSkill(data) {
                remove()
                callback()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PlayerSkill) return false
        return other.key == key
    }

    override fun toString(): String {
        return "PlayerSkill(player=${player.name}, job=$job, key=$key)"
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + privateLevel
        result = 31 * result + privateLocked.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + job.hashCode()
        result = 31 * result + level
        result = 31 * result + locked.hashCode()
        result = 31 * result + player.hashCode()
        result = 31 * result + skill.hashCode()
        return result
    }
}