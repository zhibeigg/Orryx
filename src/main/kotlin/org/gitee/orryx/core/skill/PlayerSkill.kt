package org.gitee.orryx.core.skill

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.player.OrryxPlayerSkillBindKeyEvent
import org.gitee.orryx.api.events.player.OrryxPlayerSkillCastEvents
import org.gitee.orryx.api.events.player.OrryxPlayerSkillLevelEvents
import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.mana.IManaManager
import org.gitee.orryx.core.skill.skills.PassiveSkill
import org.gitee.orryx.dao.cache.ICacheManager
import org.gitee.orryx.dao.pojo.PlayerSkill
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.castSkill
import org.gitee.orryx.utils.runCustomAction
import taboolib.common.platform.function.submitAsync
import taboolib.common5.cbool
import taboolib.module.kether.orNull

class PlayerSkill(
    override val player: Player,
    override val key: String,
    override val job: String,
    private var privateLevel: Int,
    private var privateLocked: Boolean,
    private val privateBindKeyOfGroup: MutableMap<String, String?>
): IPlayerSkill {

    override val level: Int
        get() = privateLevel

    override val locked: Boolean
        get() = privateLocked

    override val bindKeyOfGroup: Map<String, String?>
        get() = privateBindKeyOfGroup

    override val skill: ISkill
        get() = SkillLoaderManager.getSkillLoader(key)!!

    override fun cast(parameter: IParameter): CastResult {
        if (parameter !is SkillParameter) return CastResult.PARAMETER
        skill.castSkill(player, parameter)
        return CastResult.SUCCESS
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

    override fun upLevel(level: Int): SkillLevelResult {
        if (level < 0) return downLevel(-level)
        val event = OrryxPlayerSkillLevelEvents.Up(player, this, level)
        return if (event.call()) {
            var result = SkillLevelResult.SUCCESS
            if (event.upLevel <= 0) error("升级等级必须>0")
            if (privateLevel + event.upLevel > skill.maxLevel) result = SkillLevelResult.MIN
            privateLevel = (privateLevel + event.upLevel).coerceAtMost(skill.maxLevel)
            save(true)
            result
        } else {
            SkillLevelResult.CANCELLED
        }
    }

    override fun downLevel(level: Int): SkillLevelResult {
        if (level < 0) return upLevel(-level)
        val event = OrryxPlayerSkillLevelEvents.Down(player, this, level)
        return if (event.call()) {
            var result = SkillLevelResult.SUCCESS
            if (event.downLevel <= 0) error("降级等级必须>0")
            if (privateLevel - event.downLevel < skill.minLevel) result = SkillLevelResult.MIN
            privateLevel = (privateLevel - event.downLevel).coerceAtLeast(skill.minLevel)
            save(true)
            result
        } else {
            SkillLevelResult.CANCELLED
        }
    }

    override fun setLevel(level: Int): SkillLevelResult {
        return when {
            level > this.level -> upLevel(level - this.level)
            level < this.level -> downLevel(this.level - level)
            else -> SkillLevelResult.SAME
        }
    }

    override fun setBindKey(group: IGroup, bindKey: IBindKey): Boolean {
        val event = OrryxPlayerSkillBindKeyEvent(player, this, group, bindKey)
        return if (event.call()) {
            privateBindKeyOfGroup[event.group.key] = event.bindKey.key
            save(true)
            true
        } else {
            false
        }
    }

    private fun createDaoData(): PlayerSkill {
        return PlayerSkill(player.uniqueId, job, key, locked, level, bindKeyOfGroup)
    }

    override fun save(async: Boolean) {
        val data = createDaoData()
        if (async) {
            submitAsync {
                IStorageManager.INSTANCE.savePlayerSkill(player.uniqueId, data)
                ICacheManager.INSTANCE.savePlayerSkill(player.uniqueId, data, false)
            }
        } else {
            IStorageManager.INSTANCE.savePlayerSkill(player.uniqueId, data)
            ICacheManager.INSTANCE.savePlayerSkill(player.uniqueId, data, false)
        }
    }

}