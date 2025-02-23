package org.gitee.orryx.core.job

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.player.OrryxPlayerChangeGroupEvent
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobExperienceEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobLevelEvents
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillBindKeyEvent
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillUnBindKeyEvent
import org.gitee.orryx.core.experience.ExperienceLoaderManager
import org.gitee.orryx.core.experience.IExperience
import org.gitee.orryx.core.job.ExperienceResult.*
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.dao.cache.ICacheManager
import org.gitee.orryx.dao.pojo.PlayerJob
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.submitAsync
import taboolib.common5.cdouble
import taboolib.module.kether.orNull

class PlayerJob(
    override val player: Player,
    override val key: String,
    private var privateExperience: Int,
    private var privateGroup: String = DEFAULT,
    private val privateBindKeyOfGroup: MutableMap<IGroup, MutableMap<IBindKey, String?>>
): IPlayerJob {

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

    override val maxExperienceOfLevel: Int
        get() = getExperience().getExperienceOfLevel(player, experience)

    private fun createDaoData(): PlayerJob {
        return PlayerJob(player.uniqueId, key, experience, group, bindKeyOfGroup)
    }

    override fun getExperience(): IExperience {
        return ExperienceLoaderManager.getExperience(job.experience) ?: error("职业${key}的经验计算器${job.experience}找不到")
    }

    override fun getMaxMana(): Double {
        return player.eval(job.maxManaActions, mapOf("level" to level)).orNull().cdouble
    }

    override fun getReginMana(): Double {
        return player.eval(job.regainManaActions, mapOf("level" to level)).orNull().cdouble
    }

    override fun giveExperience(experience: Int): ExperienceResult {
        if (experience < 0) return takeExperience(-experience)
        val event = OrryxPlayerJobExperienceEvents.Up(player, this, experience)
        return if (event.call()) {
            val before = level
            privateExperience = (privateExperience + event.upExperience.coerceAtLeast(0)).coerceAtMost(getExperience().maxExp(player))
            val changeLevel = level - before
            if (changeLevel > 0) {
                OrryxPlayerJobLevelEvents.Up(player, this, changeLevel).call()
            }
            save(true)
            SUCCESS
        } else {
            CANCELLED
        }
    }

    override fun takeExperience(experience: Int): ExperienceResult {
        if (experience < 0) return giveExperience(-experience)
        val event = OrryxPlayerJobExperienceEvents.Down(player, this, experience)
        return if (event.call()) {
            val before = level
            privateExperience = (privateExperience - event.downExperience.coerceAtLeast(0)).coerceAtLeast(0)
            val changeLevel = before - level
            if (changeLevel > 0) {
                OrryxPlayerJobLevelEvents.Down(player, this, changeLevel).call()
            }
            save(true)
            SUCCESS
        } else {
            CANCELLED
        }
    }

    override fun setExperience(experience: Int): ExperienceResult {
        return when {
            experience > this.experience -> giveExperience(experience - this.experience)
            experience < this.experience -> takeExperience(this.experience - experience)
            else -> SAME
        }
    }

    override fun giveLevel(level: Int): LevelResult {
        if (level < 0) return takeLevel(-level)
        return when(giveExperience(getExperience().getExperienceFromTo(player, this.level, this.level + level))) {
            CANCELLED -> LevelResult.CANCELLED
            SUCCESS -> LevelResult.SUCCESS
            SAME -> LevelResult.SAME
        }
    }

    override fun takeLevel(level: Int): LevelResult {
        if (level < 0) return giveLevel(-level)
        return when(takeExperience(getExperience().getExperienceFromTo(player, this.level - level, this.level))) {
            CANCELLED -> LevelResult.CANCELLED
            SUCCESS -> LevelResult.SUCCESS
            SAME -> LevelResult.SAME
        }
    }

    override fun setLevel(level: Int): LevelResult {
        return when {
            level < this.level -> takeLevel(this.level - level)
            level > this.level -> giveLevel(level - this.level)
            else -> LevelResult.SAME
        }
    }

    override fun setGroup(group: String): Boolean {
        val iGroup = BindKeyLoaderManager.getGroup(group) ?: return false
        val event = OrryxPlayerChangeGroupEvent(player, this, iGroup)
        return if (event.call()) {
            privateGroup = event.group.key
            save(true)
            true
        } else {
            false
        }
    }

    override fun setBindKey(skill: IPlayerSkill, group: IGroup, bindKey: IBindKey): Boolean {
        val event = OrryxPlayerSkillBindKeyEvent(player, skill, group, bindKey)
        return if (event.call()) {
            privateBindKeyOfGroup.getOrPut(event.group) { mutableMapOf() }.apply {
                replaceAll { _, u ->
                    if (u == skill.key) {
                        null
                    } else {
                        u
                    }
                }
                set(bindKey, skill.key)
            }
            save(true)
            true
        } else {
            false
        }
    }

    override fun unBindKey(skill: IPlayerSkill, group: IGroup): Boolean {
        val event = OrryxPlayerSkillUnBindKeyEvent(player, skill, group)
        return if (event.call()) {
            privateBindKeyOfGroup[event.group]?.replaceAll { _, u ->
                if (u == skill.key) {
                    null
                } else {
                    u
                }
            }
            save(true)
            true
        } else {
            false
        }
    }

    override fun save(async: Boolean) {
        val data = createDaoData()
        if (async) {
            submitAsync {
                IStorageManager.INSTANCE.savePlayerJob(player.uniqueId, data)
                ICacheManager.INSTANCE.savePlayerJob(player.uniqueId, data, false)
            }
        } else {
            IStorageManager.INSTANCE.savePlayerJob(player.uniqueId, data)
            ICacheManager.INSTANCE.savePlayerJob(player.uniqueId, data, false)
        }
    }

}