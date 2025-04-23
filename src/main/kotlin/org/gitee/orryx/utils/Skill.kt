package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.events.player.skill.OrryxClearSkillLevelAndBackPointEvent
import org.gitee.orryx.api.events.player.state.OrryxPlayerStateSkillEvents
import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.kether.KetherScript
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.skill.*
import org.gitee.orryx.core.skill.skills.DirectAimSkill
import org.gitee.orryx.core.skill.skills.DirectSkill
import org.gitee.orryx.core.skill.skills.PressingAimSkill
import org.gitee.orryx.core.skill.skills.PressingSkill
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.pipe.PipeBuilder
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.module.state.StateManager.statusData
import org.gitee.orryx.module.state.states.SkillState
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common5.cdouble
import taboolib.common5.clong
import taboolib.module.kether.extend
import taboolib.module.kether.orNull
import java.util.*
import java.util.concurrent.CompletableFuture

const val DIRECT = "Direct"
const val DIRECT_AIM = "Direct Aim"
const val PRESSING = "Pressing"
const val PRESSING_AIM = "Pressing Aim"
const val PASSIVE = "Passive"

const val DEFAULT_PICTURE = "default"

val silence: Boolean by ConfigLazy(Orryx.config) { Orryx.config.getBoolean("Silence", false) }

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

fun IPlayerSkill.up(): CompletableFuture<SkillLevelResult> {
    if (level == skill.maxLevel) return CompletableFuture.completedFuture(SkillLevelResult.MAX)
    val from = level
    val to = level+1
    val future = CompletableFuture<SkillLevelResult>()
    player.orryxProfile { profile ->
        if (upLevelCheck(from, to)) {
            upgradePointCheck(from, to).thenApply { pointCheck ->
                if (pointCheck.second) {
                    val result = upLevel(1)
                    result.thenApply {
                        if (it == SkillLevelResult.SUCCESS) {
                            profile.takePoint(pointCheck.first)
                            upLevelSuccess(from, level)
                        }
                        future.complete(it)
                    }
                } else {
                    future.complete(SkillLevelResult.POINT)
                }
            }
        } else {
            future.complete(SkillLevelResult.CHECK)
        }
    }
    return future
}

fun <T> Player.skill(skill: String, create: Boolean = false, function: (IPlayerSkill) -> T): CompletableFuture<T?> {
    return getSkill(skill, create).thenApply {
        it?.let { it1 -> function(it1) }
    }
}

internal fun Player.getSkill(job: IPlayerJob, skill: String, create: Boolean = false): CompletableFuture<IPlayerSkill?> {
    return getSkill(job.key, skill, create)
}

fun <T> Player.skill(job: IPlayerJob, skill: String, create: Boolean = false, function: (IPlayerSkill) -> T): CompletableFuture<T?> {
    return getSkill(job, skill, create).thenApply {
        it?.let { it1 -> function(it1) }
    }
}

fun <T> Player.skill(job: String, skill: String, create: Boolean = false, function: (IPlayerSkill) -> T): CompletableFuture<T?> {
    return getSkill(job, skill, create).thenApply {
        it?.let { it1 -> function(it1) }
    }
}

internal fun Player.getSkill(skill: String, create: Boolean = false): CompletableFuture<IPlayerSkill?> {
    val future = CompletableFuture<IPlayerSkill?>()
    job {
        getSkill(it.key, skill, create).thenApply { skill ->
            future.complete(skill)
        }
    }
    return future
}

internal fun Player.getSkill(job: String, skill: String, create: Boolean = false): CompletableFuture<IPlayerSkill?> {
    val skillLoader = SkillLoaderManager.getSkillLoader(skill)
    return MemoryCache.getPlayerSkill(this, job, skill).thenApply {
        skillLoader ?: return@thenApply null
        it ?: if (create) {
            PlayerSkill(this, skill, job, skillLoader.minLevel, skillLoader.isLocked).apply {
                save(isPrimaryThread)
            }
        } else {
            null
        }
    }
}

fun Player.getGroupSkills(group: String): CompletableFuture<Map<IBindKey, String?>?> {
    return job { job ->
        BindKeyLoaderManager.getGroup(group)?.let { group ->
            job.bindKeyOfGroup[group]
        } ?: emptyMap()
    }
}

internal fun IPlayerSkill.parameter(): SkillParameter {
    return SkillParameter(key, player, level)
}

fun IPlayerSkill.getDescriptionComparison(): List<String> {
    return skill.description.getDescriptionComparison(player, SkillParameter(key, player, level))
}

fun IPlayerSkill.getIcon(): String {
    return skill.icon.getIcon(player, SkillParameter(key, player, level))
}

fun ICastSkill.consume(player: Player, parameter: SkillParameter) {
    silence(parameter, player)
    SkillTimer.reset(player, parameter)
}

fun ISkill.castSkill(player: Player, parameter: SkillParameter, consume: Boolean = true) {
    when(val skill = this) {
        is PressingSkill -> {
            if (PressSkillManager.pressTaskMap.containsKey(player.uniqueId)) return
            val maxPressTick = parameter.runCustomAction(skill.maxPressTickAction).orNull().clong
            val time = System.currentTimeMillis()
            PressSkillManager.pressTaskMap[player.uniqueId] = key to PipeBuilder()
                .uuid(UUID.randomUUID())
                .timeout(maxPressTick)
                .periodTask(period) {
                    parameter.runCustomAction(skill.pressPeriodAction, mapOf("pressTick" to (System.currentTimeMillis() - time) / 50))
                }.onComplete {
                    if (consume) skill.consume(player, parameter)
                    parameter.runSkillAction(mapOf("pressTick" to maxPressTick))
                    PressSkillManager.pressTaskMap.remove(player.uniqueId)
                    CompletableFuture.completedFuture(null)
                }.onBrock {
                    if (consume) skill.consume(player, parameter)
                    parameter.runSkillAction(mapOf("pressTick" to (System.currentTimeMillis() - time) / 50))
                    PressSkillManager.pressTaskMap.remove(player.uniqueId)
                    CompletableFuture.completedFuture(null)
                }.build()
        }
        is PressingAimSkill -> {
            val aimRadius = parameter.runCustomAction(skill.aimRadiusAction).orNull().cdouble
            val aimMin = parameter.runCustomAction(skill.aimMinAction).orNull().cdouble
            val aimMax = parameter.runCustomAction(skill.aimMaxAction).orNull().cdouble
            val maxTick = parameter.runCustomAction(skill.maxPressTickAction).orNull().clong
            val timestamp = System.currentTimeMillis()
            PluginMessageHandler.requestAiming(player, key, DEFAULT_PICTURE, aimMin, aimMax, aimRadius, maxTick) { aimInfo ->
                aimInfo.getOrNull()?.let {
                    if (it.skillId == skill.key) {
                        if (consume) skill.consume(player, parameter)
                        parameter.origin = it.location.toTarget()
                        parameter.runSkillAction(
                            mapOf(
                                "aimRadius" to aimRadius,
                                "aimMin" to aimMin,
                                "aimMax" to aimMax,
                                "pressTick" to (it.timestamp - timestamp)/50
                            )
                        )
                    }
                }
            }
        }
        is DirectSkill -> {
            if (consume) skill.consume(player, parameter)
            parameter.runSkillAction()
        }
        is DirectAimSkill -> {
            val aimRadius = parameter.runCustomAction(skill.aimRadiusAction).orNull().cdouble
            val aimSize = parameter.runCustomAction(skill.aimSizeAction).orNull().cdouble
            PluginMessageHandler.requestAiming(player, key, DEFAULT_PICTURE, aimSize, aimRadius) { aimInfo ->
                aimInfo.onSuccess {
                    if (it.skillId == skill.key) {
                        parameter.origin = it.location.toTarget()
                        if (consume) skill.consume(player, parameter)
                        parameter.runSkillAction(
                            mapOf(
                                "aimRadius" to aimRadius,
                                "aimSize" to aimSize
                            )
                        )
                    }
                }
            }
        }
    }
}

fun IPlayerSkill.tryCast(): CompletableFuture<CastResult> {
    val parameter = parameter()
    val result = castCheck(parameter)
    result.thenAccept {
        if (!silence) it.sendLang(player)
        if (it.isSuccess()) {
            cast(parameter, true)
        }
    }
    return result
}

fun CastResult.isSuccess(): Boolean {
    return this == CastResult.SUCCESS
}

fun IPlayerSkill.clearLevelAndBackPoint(): CompletableFuture<Boolean> {
    val future = CompletableFuture<Boolean>()
    if (level == skill.minLevel) {
        future.complete(false)
    } else {
        player.orryxProfile { profile ->
            val event = OrryxClearSkillLevelAndBackPointEvent(player, this)
            if (event.call()) {
                upgradePointCheck(skill.minLevel, level).thenApply { pair ->
                    profile.givePoint(pair.first)
                    clearLevel().thenApply {
                        future.complete(it == SkillLevelResult.SUCCESS)
                    }
                }
            } else {
                future.complete(false)
            }
        }
    }
    return future
}

fun IPlayerSkill.clearLevel(): CompletableFuture<SkillLevelResult> {
    return setLevel(skill.minLevel)
}

fun ICastSkill.silence(skillParameter: SkillParameter, player: Player): Long {
    val timeout = skillParameter.silenceValue(true)
    if (timeout >= 0) {
        player.statusData().also {
            val state = SkillState.Running(it, StateManager.getGlobalState(key)!! as SkillState, timeout / 50)
            val event = OrryxPlayerStateSkillEvents.Pre(player, skillParameter, timeout, state)
            if (event.call()) {
                it.next(event.state)
                Orryx.api().profileAPI.setSilence(player, event.silence)
                OrryxPlayerStateSkillEvents.Post(player, skillParameter, event.silence, event.state).call()
            }
            return event.silence
        }
    } else {
        Orryx.api().profileAPI.cancelSilence(player)
        return 0
    }
}