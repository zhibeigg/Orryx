package org.gitee.orryx.core.skill

import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCastEvents
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillClearEvents
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillLevelEvents
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillSaveEvents
import org.gitee.orryx.command.OrryxTestCommand
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.skill.skills.PassiveSkill
import org.gitee.orryx.dao.cache.ISyncCacheManager
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.persistence.PersistenceManager
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.utils.Tuple2
import org.gitee.orryx.utils.castSkill
import org.gitee.orryx.utils.castSkillAsync
import org.gitee.orryx.utils.castSkillRawAsync
import org.gitee.orryx.utils.finishSaveCallback
import org.gitee.orryx.utils.mainThreadFuture
import org.gitee.orryx.utils.thenApplyMain
import org.gitee.orryx.utils.orryxProfileTo
import org.gitee.orryx.utils.runCustomAction
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common5.cbool
import taboolib.common5.cint
import taboolib.module.kether.orNull
import java.util.*
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
        get() = SkillLoaderManager.getSkillLoader(key) ?: error("Skill '$key' not found")

    override fun cast(parameter: IParameter, consume: Boolean): CastResult {
        if (parameter !is SkillParameter) return CastResult.PARAMETER
        if (!OrryxPlayerSkillCastEvents.Cast(player, this, parameter).call()) return CastResult.CANCELED
        val shouldConsume = consume && !OrryxTestCommand.isUnlimited(player)
        skill.castSkillAsync(player, parameter, shouldConsume).exceptionally {
            it.printStackTrace()
            null
        }
        return CastResult.SUCCESS
    }

    internal fun castAsync(parameter: SkillParameter, consume: Boolean): CompletableFuture<CastResult> {
        return mainThreadFuture {
            if (!OrryxPlayerSkillCastEvents.Cast(player, this, parameter).call()) {
                return@mainThreadFuture false
            }
            true
        }.thenCompose { allowed ->
            if (!allowed) {
                CompletableFuture.completedFuture(CastResult.CANCELED)
            } else {
                skill.castSkillRawAsync(player, parameter, consume && !OrryxTestCommand.isUnlimited(player))
            }
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
        return IManaManager.INSTANCE.haveMana(player, parameter.manaValue()).thenApplyMain { mana ->
            if (!mana) return@thenApplyMain CastResult.MANA_NOT_ENOUGH
            // 脚本检测
            if ((skill as? ICastSkill)?.castCheckAction?.let { parameter.runCustomAction(it, mapOf()).orNull().cbool } == false) return@thenApplyMain CastResult.CHECK_ACTION_FAILED
            // 事件
            if (!OrryxPlayerSkillCastEvents.Check(player, this, parameter).call()) return@thenApplyMain CastResult.CANCELED
            return@thenApplyMain CastResult.SUCCESS
        }
    }

    override fun upLevelCheck(from: Int, to: Int): Boolean {
        skill.upLevelCheckAction?.let {
            return runCustomAction(it, mapOf("from" to from, "to" to to)).orNull().cbool
        }
        return true
    }

    override fun downLevelCheck(from: Int, to: Int): Boolean {
        skill.downLevelCheckAction?.let {
            return runCustomAction(it, mapOf("from" to from, "to" to to)).orNull().cbool
        }
        return true
    }

    override fun upLevelSuccess(from: Int, to: Int) {
        skill.upLevelSuccessAction?.let {
            runCustomAction(it, mapOf("from" to from, "to" to to))
        }
    }

    override fun downLevelSuccess(from: Int, to: Int) {
        skill.downLevelSuccessAction?.let {
            runCustomAction(it, mapOf("from" to from, "to" to to))
        }
    }

    override fun upgradePointCheck(from: Int, to: Int): CompletableFuture<Tuple2<Int,Boolean>> {
        val point = skill.upgradePointAction?.let { runCustomAction(it, mapOf("from" to from, "to" to to)).orNull() }.cint
        return player.orryxProfileTo {
            Tuple2(point, (it.point >= point))
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
            persist(isPrimaryThread, false).whenComplete { _, throwable ->
                org.gitee.orryx.utils.runOnMainThread {
                    if (throwable != null) future.completeExceptionally(throwable) else {
                        OrryxPlayerSkillLevelEvents.Up.Post(player, this, event.upLevel).call()
                        future.complete(result)
                    }
                }
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
            persist(isPrimaryThread, false).whenComplete { _, throwable ->
                org.gitee.orryx.utils.runOnMainThread {
                    if (throwable != null) future.completeExceptionally(throwable) else {
                        OrryxPlayerSkillLevelEvents.Down.Post(player, this, event.downLevel).call()
                        future.complete(result)
                    }
                }
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
        return PlayerSkillPO(id, uuid, job, key, locked, level)
    }

    override fun clear(): CompletableFuture<Boolean> {
        val event = OrryxPlayerSkillClearEvents.Pre(player, this)
        val future = CompletableFuture<Boolean>()
        if (event.call()) {
            privateLocked = skill.isLocked
            privateLevel = skill.minLevel
            persist(isPrimaryThread, false).whenComplete { _, throwable ->
                org.gitee.orryx.utils.runOnMainThread {
                    if (throwable != null) future.completeExceptionally(throwable) else {
                        OrryxPlayerSkillClearEvents.Post(player, this).call()
                        future.complete(true)
                    }
                }
            }
        } else {
            future.complete(false)
        }
        return future
    }

    /**
     * 仅重置内存状态，不执行数据库保存。
     * 供 [org.gitee.orryx.core.job.PlayerJob.clear] 批量收集 PO 后统一事务保存。
     * @return Pre 事件是否通过
     */
    internal fun clearMemoryState(): Boolean {
        val event = OrryxPlayerSkillClearEvents.Pre(player, this)
        if (event.call()) {
            privateLocked = skill.isLocked
            privateLevel = skill.minLevel
            return true
        }
        return false
    }

    override fun save(async: Boolean, remove: Boolean, callback: Runnable) {
        persist(async, remove).whenComplete { context, throwable ->
            finishSaveCallback(callback, throwable) {
                OrryxPlayerSkillSaveEvents.Post(context.player, this@PlayerSkill, context.async, context.remove).call()
            }
        }
    }

    private fun persist(async: Boolean, remove: Boolean): CompletableFuture<SaveContext> {
        return org.gitee.orryx.utils.mainThreadFuture {
            val onlinePlayer = player
            val event = OrryxPlayerSkillSaveEvents.Pre(onlinePlayer, this, async, remove)
            event.call()
            SaveContext(onlinePlayer, event.async, event.remove, createPO())
        }.thenCompose { context ->
            PersistenceManager.saveSkill(context.data, context.remove).thenApply { context }
        }
    }

    private data class SaveContext(
        val player: Player,
        val async: Boolean,
        val remove: Boolean,
        val data: PlayerSkillPO,
    )

    override fun equals(other: Any?): Boolean {
        if (other !is PlayerSkill) return false
        return other.key == key
    }

    override fun toString(): String {
        return "PlayerSkill(player=${player.name}, job=$job, key=$key)"
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}
