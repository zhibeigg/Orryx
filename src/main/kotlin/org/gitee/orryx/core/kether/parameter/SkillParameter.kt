package org.gitee.orryx.core.kether.parameter

import org.bukkit.Location
import org.bukkit.entity.Player
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.utils.toTarget
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.warning
import taboolib.common5.cdouble
import taboolib.common5.clong
import taboolib.module.kether.orNull

class SkillParameter(val skill: String?, val player: Player, var level: Int = 1): IParameter {

    constructor(skillParameter: SkillParameter, origin: ITargetLocation<*>?): this(skillParameter.skill, skillParameter.player, skillParameter.level) {
        this.origin = origin ?: player.toTarget()
        this.trigger = skillParameter.trigger
    }

    override var origin: ITargetLocation<*>? = player.toTarget()

    /**
     * 技能触发方式。
     */
    var trigger: SkillTrigger = SkillTrigger.Unknown

    /**
     * 获取触发的按键绑定（如果是按键触发）。
     *
     * @return 按键绑定，如果不是按键触发则返回 null
     */
    fun getBindKey(): IBindKey? {
        return (trigger as? SkillTrigger.Key)?.bindKey
    }

    /**
     * 构建触发方式相关的变量 Map。
     *
     * @return 包含触发方式信息的变量 Map
     */
    fun buildTriggerVariables(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "triggerType" to trigger.name
        )
        when (val t = trigger) {
            is SkillTrigger.Key -> {
                map["bindKey"] = t.bindKey
                map["bindKeyName"] = t.bindKey.key
            }
            is SkillTrigger.Command -> {
                map["triggerCommand"] = t.command
            }
            is SkillTrigger.Api -> {
                map["triggerSource"] = t.source
            }
            else -> {}
        }
        return map
    }

    private val proxyCommandSender by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { adaptPlayer(player) }

    private val lazies by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { hashMapOf<String, Any?>() }

    fun getSkill(): ISkill? {
        return skill?.let { SkillLoaderManager.getSkillLoader(skill) }
    }

    fun getOriginLocation(): Location {
        return origin?.location ?: player.location
    }

    override fun getVariable(key: String, lazy: Boolean): Any? {
        fun getAndSetValue(): Any? {
            val value = getSkill()?.variables?.get(key)?.let {
                ScriptManager.runScript(proxyCommandSender, this, it) {
                    set("level", level)
                }.orNull()
            }
            if (value == null) {
                warning("未找到技能 $skill 的变量 $key ")
                return null
            } else {
                lazies[key] = value
                return value
            }
        }
        return if (lazy) {
            lazies[key] ?: getAndSetValue()
        } else {
            getAndSetValue()
        }
    }

    override fun getVariable(key: String, default: Any): Any {
        return lazies[key] ?: lazies.put(key, default) ?: default
    }

    fun manaValue(lazy: Boolean = false): Double {
        return getVariable("MANA", lazy).cdouble
    }

    fun cooldownValue(lazy: Boolean = false): Long {
        return getVariable("COOLDOWN", lazy).clong * 50
    }

    fun silenceValue(lazy: Boolean = false): Long {
        return getVariable("SILENCE", lazy).clong * 50
    }

    override fun toString(): String {
        return "SkillParameter{skill=$skill, player=${player.name}, level=$level, trigger=${trigger.name}}"
    }
}