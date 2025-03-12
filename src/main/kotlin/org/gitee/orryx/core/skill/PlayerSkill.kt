package org.gitee.orryx.core.skill

import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI.saveScope
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCastEvents
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillLevelEvents
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.mana.IManaManager
import org.gitee.orryx.core.profile.PlayerProfileManager.orryxProfile
import org.gitee.orryx.core.skill.skills.PassiveSkill
import org.gitee.orryx.dao.cache.ICacheManager
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.castSkill
import org.gitee.orryx.utils.runCustomAction
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common5.cbool
import taboolib.common5.cint
import taboolib.module.kether.orNull
import java.util.concurrent.CompletableFuture

class PlayerSkill(
    override val player: Player,
    override val key: String,
    override val job: String,
    private var privateLevel: Int,
    private var privateLocked: Boolean
): IPlayerSkill {

    override val level: Int
        get() = privateLevel

    override val locked: Boolean
        get() = privateLocked

    override val skill: ISkill by lazy { SkillLoaderManager.getSkillLoader(key)!! }

    override fun cast(parameter: IParameter, consume: Boolean): CastResult {
        if (parameter !is SkillParameter) return CastResult.PARAMETER
        return if (OrryxPlayerSkillCastEvents.Cast(player, this, parameter).call()) {
            skill.castSkill(player, parameter, consume)
            CastResult.SUCCESS
        } else {
            CastResult.CANCELED
        }
    }

    override fun castCheck(parameter: IParameter): CastResult {
        if (parameter !is SkillParameter) return CastResult.PARAMETER
        val skill = skill
        //被动技能
        if (skill is PassiveSkill) return CastResult.PASSIVE
        //冷却
        if (!SkillTimer.hasNext(player, key)) return CastResult.COOLDOWN
        //法力
        if (!IManaManager.INSTANCE.haveMana(player, parameter.manaValue())) return CastResult.MANA_NOT_ENOUGH
        //脚本检测
        if ((skill as? ICastSkill)?.castCheckAction?.let { runCustomAction(it, mapOf()).orNull().cbool } == false) return CastResult.CHECK_ACTION_FAILED
        //事件
        if (!OrryxPlayerSkillCastEvents.Check(player, this, parameter).call()) return CastResult.CANCELED
        return CastResult.SUCCESS
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

    override fun upgradePointCheck(from: Int, to: Int): Pair<Int,Boolean> {
        val point = skill.upgradePointAction?.let { runCustomAction(it, mapOf("from" to from, "to" to to)).orNull() }.cint
        return point to (player.orryxProfile().point >= point)
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

    private fun createDaoData(): PlayerSkillPO {
        return PlayerSkillPO(player.uniqueId, job, key, locked, level)
    }

    override fun save(async: Boolean, callback: () -> Unit) {
        val data = createDaoData()
        if (async && !GameManager.shutdown) {
            saveScope.launch {
                IStorageManager.INSTANCE.savePlayerSkill(player.uniqueId, data)
                ICacheManager.INSTANCE.savePlayerSkill(player.uniqueId, data, false)
            }.invokeOnCompletion {
                callback()
            }
        } else {
            IStorageManager.INSTANCE.savePlayerSkill(player.uniqueId, data)
            ICacheManager.INSTANCE.savePlayerSkill(player.uniqueId, data, false)
            callback()
        }
    }

}