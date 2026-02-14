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
import org.gitee.orryx.core.kether.parameter.SkillTrigger
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.skill.*
import org.gitee.orryx.core.skill.caster.SkillCasterRegistry
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.module.state.StateManager.statusData
import org.gitee.orryx.module.state.states.SkillState
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.configuration.util.ReloadAwareLazy
import taboolib.module.kether.extend
import java.util.concurrent.CompletableFuture

const val DIRECT = "Direct"
const val DIRECT_AIM = "Direct Aim"
const val PRESSING = "Pressing"
const val PRESSING_AIM = "Pressing Aim"
const val PASSIVE = "Passive"

const val DEFAULT_PICTURE = "default"

val silence: Boolean by ReloadAwareLazy(Orryx.config) { Orryx.config.getBoolean("Silence", false) }

internal fun SkillParameter.runSkillAction(map: Map<String, Any?> = emptyMap()): CompletableFuture<Any?>? {
    return SkillLoaderManager.getSkillLoader(skill ?: return CompletableFuture.completedFuture(null))?.let { skill ->
        skill as ICastSkill
        val combinedMap = buildTriggerVariables() + map
        KetherScript(skill.key, skill.script ?: error("请修复技能配置中的错误${skill.key}")).runActions(
            this,
            combinedMap
        )
    }
}

internal fun SkillParameter.runSkillExtendAction(
    extend: String,
    map: Map<String, Any?> = emptyMap()
): CompletableFuture<Any?>? {
    return SkillLoaderManager.getSkillLoader(skill ?: return CompletableFuture.completedFuture(null))?.let { skill ->
        skill as ICastSkill
        val combinedMap = buildTriggerVariables() + map
        KetherScript(
            skill.key,
            skill.extendScripts[extend] ?: error("请修复技能配置中的错误${skill.key} extend $extend")
        ).runExtendActions(this, extend, combinedMap)
    }
}

internal fun IPlayerSkill.runSkillAction(map: Map<String, Any> = emptyMap()) {
    (skill as? ICastSkill)?.let { skill ->
        KetherScript(key, skill.script ?: error("请修复技能配置中的错误$key")).runActions(
            SkillParameter(
                key,
                player,
                level
            ), map
        )
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

internal fun SkillParameter.runCustomAction(
    action: String,
    map: Map<String, Any> = emptyMap()
): CompletableFuture<Any?> {
    return ScriptManager.runScript(adaptPlayer(player), this, action) {
        extend(map)
    }
}

fun IPlayerSkill.up(): CompletableFuture<SkillLevelResult> {
    if (level == skill.maxLevel) return CompletableFuture.completedFuture(SkillLevelResult.MAX)
    val from = level
    val to = level + 1
    return player.orryxProfile { profile ->
        if (upLevelCheck(from, to)) {
            upgradePointCheck(from, to).thenCompose { pointCheck ->
                if (pointCheck.second) {
                    val result = upLevel(1)
                    result.thenApply {
                        if (it == SkillLevelResult.SUCCESS) {
                            profile.takePoint(pointCheck.first)
                            upLevelSuccess(from, level)
                        }
                        it
                    }
                } else {
                    CompletableFuture.completedFuture(SkillLevelResult.POINT)
                }
            }
        } else {
            CompletableFuture.completedFuture(SkillLevelResult.CHECK)
        }
    }
}

fun IPlayerSkill.down(): CompletableFuture<SkillLevelResult> {
    if (level == skill.minLevel) return CompletableFuture.completedFuture(SkillLevelResult.MIN)
    val from = level
    val to = level - 1
    return player.orryxProfile { profile ->
        if (downLevelCheck(from, to)) {
            upgradePointCheck(to, from).thenCompose { pointCheck ->
                if (pointCheck.second) {
                    val result = downLevel(1)
                    result.thenApply {
                        if (it == SkillLevelResult.SUCCESS) {
                            profile.givePoint(pointCheck.first)
                            downLevelSuccess(from, level)
                        }
                        it
                    }
                } else {
                    CompletableFuture.completedFuture(SkillLevelResult.POINT_REFUND)
                }
            }
        } else {
            CompletableFuture.completedFuture(SkillLevelResult.CHECK)
        }
    }
}

inline fun <T> Player.skill(
    skill: String,
    create: Boolean = false,
    crossinline function: (IPlayerSkill) -> T
): CompletableFuture<T?> {
    return getSkill(skill, create).thenApply {
        it?.let { it1 -> function(it1) }
    }
}

fun Player.getSkill(job: IPlayerJob, skill: String, create: Boolean = false): CompletableFuture<IPlayerSkill?> {
    return getSkill(job.key, skill, create)
}

inline fun <T> Player.skill(
    job: IPlayerJob,
    skill: String,
    create: Boolean = false,
    crossinline function: (IPlayerSkill) -> T
): CompletableFuture<T?> {
    return getSkill(job, skill, create).thenApply {
        it?.let { it1 -> function(it1) }
    }
}

inline fun <T> Player.skill(
    job: String,
    skill: String,
    create: Boolean = false,
    crossinline function: (IPlayerSkill) -> T
): CompletableFuture<T?> {
    return getSkill(job, skill, create).thenApply {
        it?.let { it1 -> function(it1) }
    }
}

fun Player.getSkill(skill: String, create: Boolean = false): CompletableFuture<IPlayerSkill?> {
    val future = CompletableFuture<IPlayerSkill?>()
    job {
        getSkill(it.key, skill, create).thenApply { skill ->
            future.complete(skill)
        }
    }
    return future
}

fun Player.getSkill(job: String, skill: String, create: Boolean = false): CompletableFuture<IPlayerSkill?> {
    val skillLoader = SkillLoaderManager.getSkillLoader(skill) ?: return CompletableFuture.completedFuture(null)
    return orryxProfile { profile ->
        MemoryCache.getPlayerSkill(uniqueId, profile.id, job, skill).thenApply {
            it ?: if (create) {
                PlayerSkill(profile.id, uniqueId, skill, job, skillLoader.minLevel, skillLoader.isLocked).apply {
                    save(remove = false)
                }
            } else {
                null
            }
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
    IManaManager.INSTANCE.takeMana(player, parameter.manaValue(true))
}

fun ISkill.castSkill(player: Player, parameter: SkillParameter, consume: Boolean = true) {
    debug { "玩家 ${player.name} cast skill $key" }
    SkillCasterRegistry.getCaster(this)?.cast(this, player, parameter, consume)
}

fun IPlayerSkill.tryCast(trigger: SkillTrigger = SkillTrigger.Unknown): CompletableFuture<CastResult> {
    debug { "玩家 ${player.name} try cast skill $key" }
    val parameter = parameter().apply { this.trigger = trigger }
    val result = castCheck(parameter)
    result.thenAccept {
        debug { "玩家 ${player.name} try cast skill result ${it.name}" }
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
    return if (level == skill.minLevel) {
        CompletableFuture.completedFuture(false)
    } else {
        player.orryxProfile { profile ->
            val event = OrryxClearSkillLevelAndBackPointEvent(player, this)
            if (event.call()) {
                upgradePointCheck(skill.minLevel, level).thenCompose { pair ->
                    profile.givePoint(pair.first)
                    clearLevel().thenApply {
                        it == SkillLevelResult.SUCCESS
                    }
                }
            } else {
                CompletableFuture.completedFuture(false)
            }
        }
    }
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