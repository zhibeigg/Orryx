package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.KetherScript
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.skill.*
import org.gitee.orryx.core.skill.skills.DirectAimSkill
import org.gitee.orryx.core.skill.skills.DirectSkill
import org.gitee.orryx.core.skill.skills.PressingAimSkill
import org.gitee.orryx.core.skill.skills.PressingSkill
import org.gitee.orryx.dao.cache.ICacheManager
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.cdouble
import taboolib.module.kether.extend
import taboolib.module.kether.orNull
import java.util.concurrent.CompletableFuture

const val DIRECT = "Direct"
const val DIRECT_AIM = "Direct Aim"
const val PRESSING = "Pressing"
const val PRESSING_AIM = "Pressing Aim"
const val PASSIVE = "Passive"

const val DEFAULT_PICTURE = "default"

internal fun SkillParameter.runSkillAction(map: Map<String, Any> = emptyMap()) {
    SkillLoaderManager.getSkillLoader(skill ?: return)?.let { skill ->
        if (skill is ICastSkill) {
            KetherScript(skill.key, skill.script ?: error("请修复技能配置中的错误${skill.key}")).runActions(this, map)
        }
    }
}

internal fun IPlayerSkill.runSkillAction(map: Map<String, Any> = emptyMap()) {
    (skill as? ICastSkill)?.let { skill ->
        KetherScript(key, skill.script ?: error("请修复技能配置中的错误$key")).runActions(SkillParameter(key, player, level), map)
    }
}

internal fun ICastSkill.runSkillAction(player: Player, level: Int, map: Map<String, Any> = emptyMap()) {
    KetherScript(key, script ?: error("请修复技能配置中的错误$key")).runActions(SkillParameter(key, player, level), map)
}

internal fun IPlayerSkill.runCustomAction(action: String, map: Map<String, Any> = emptyMap()): CompletableFuture<Any?> {
    return ScriptManager.runScript(adaptPlayer(player), SkillParameter(key, player, level), action) {
        extend(map)
    }
}

internal fun SkillParameter.runCustomAction(action: String, map: Map<String, Any> = emptyMap()): CompletableFuture<Any?> {
    return ScriptManager.runScript(adaptPlayer(player), this, action) {
        extend(map)
    }
}

internal fun PlayerSkill.up() {
    val from = level
    val to = level+1
    if (upLevelCheck(from, to) && upLevel(1) == SkillLevelResult.SUCCESS) {
        upLevelSuccess(from, level)
    }
}

internal fun Player.getSkill(job: String, skill: String): IPlayerSkill? {
    return ICacheManager.INSTANCE.getPlayerSkill(uniqueId, job, skill)?.let { PlayerSkill(this, skill, job, it.level, it.locked) }
}

internal fun IPlayerSkill.parameter(): IParameter {
    return SkillParameter(key, player, level)
}

fun ISkill.castSkill(player: Player, parameter: SkillParameter) {
    when(val skill = this) {
        is PressingSkill -> parameter.runSkillAction(mapOf("pressTick" to 1))
        is PressingAimSkill -> {
            val aimRange = parameter.runCustomAction(skill.aimRangeAction).orNull().cdouble
            val aimScale = parameter.runCustomAction(skill.aimScaleAction).orNull().cdouble
            PluginMessageHandler.requestAiming(player, key, DEFAULT_PICTURE, aimScale, aimRange) { aimInfo ->
                aimInfo.getOrNull()?.let {
                    if (it.skillId == skill.key) {
                        parameter.origin = it.location.toTarget()
                        parameter.runSkillAction(
                            mapOf(
                                "aimRange" to aimRange,
                                "aimScale" to aimScale,
                                "pressTick" to 1
                            )
                        )
                    }
                }
            }
        }
        is DirectSkill -> parameter.runSkillAction()
        is DirectAimSkill -> {
            val aimRange = parameter.runCustomAction(skill.aimRangeAction).orNull().cdouble
            val aimScale = parameter.runCustomAction(skill.aimScaleAction).orNull().cdouble
            PluginMessageHandler.requestAiming(player, key, DEFAULT_PICTURE, aimScale, aimRange) { aimInfo ->
                aimInfo.getOrNull()?.let {
                    if (it.skillId == skill.key) {
                        parameter.origin = it.location.toTarget()
                        parameter.runSkillAction(
                            mapOf(
                                "aimRange" to aimRange,
                                "aimScale" to aimScale
                            )
                        )
                    }
                }
            }
        }
    }
}

fun CastResult.isSuccess(): Boolean {
    return this == CastResult.SUCCESS
}