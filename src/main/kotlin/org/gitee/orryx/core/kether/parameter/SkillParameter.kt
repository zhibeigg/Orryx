package org.gitee.orryx.core.kether.parameter

import org.bukkit.Location
import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.utils.toTarget
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.warning
import taboolib.common5.cdouble
import taboolib.common5.clong
import taboolib.module.kether.orNull

class SkillParameter(val skill: String?, val player: Player, var level: Int = 1): IParameter {

    override var origin: ITarget<*>? = player.toTarget()

    private val proxyCommandSender by lazy { adaptPlayer(player) }

    private val lazies by lazy { mutableMapOf<String, Any?>() }

    fun getSkill(): ISkill? {
        return skill?.let { SkillLoaderManager.getSkillLoader(skill) }
    }

    fun getOriginLocation(): Location {
        return origin?.location ?: player.location
    }

    override fun getVariable(key: String, lazy: Boolean): Any? {
        fun getAndSetValue(): Any? {
            val value = getSkill()?.variables?.get(key)?.let { ScriptManager.runScript(proxyCommandSender, this, it).orNull() }
            if (value == null) {
                warning("未找到技能${skill}的变量$key")
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

    fun manaValue(lazy: Boolean = false): Double {
        return getVariable("MANA", lazy).cdouble
    }

    fun cooldownValue(lazy: Boolean = false): Long {
        return getVariable("COOLDOWN", lazy).clong * 50
    }

    override fun toString(): String {
        return "SkillParameter{skill=$skill, player=${player.name}, level=$level}"
    }

}