package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.ProfileAPI
import org.gitee.orryx.api.TimedStatusApplication
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
import org.gitee.orryx.dao.persistence.PersistenceManager
import java.util.concurrent.ConcurrentHashMap
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

internal data class SkillSilenceApplication(
    val timeout: Long,
    val plannedState: SkillState.Running?,
    val timedStatus: TimedStatusApplication?,
)

val silence: Boolean by ReloadAwareLazy(Orryx.config) { Orryx.config.getBoolean("Silence", false) }

private val pendingSkillCreations = ConcurrentHashMap<String, CompletableFuture<IPlayerSkill?>>()

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

internal fun SkillParameter.startSkillAction(map: Map<String, Any?> = emptyMap()): CompletableFuture<Unit> {
    val loader = SkillLoaderManager.getSkillLoader(skill ?: return CompletableFuture.completedFuture(Unit))
        ?: return CompletableFuture.completedFuture(Unit)
    val castSkill = loader as? ICastSkill
        ?: return CompletableFuture<Unit>().also {
            it.completeExceptionally(IllegalArgumentException("技能 ${loader.key} 不是可释放技能"))
        }
    val combinedMap = buildTriggerVariables() + map
    return KetherScript(castSkill.key, castSkill.script ?: error("请修复技能配置中的错误${castSkill.key}"))
        .startActions(this, combinedMap)
}

internal fun SkillParameter.finishConsumption(
    consumption: StartupConsumption,
    startup: () -> CompletableFuture<Unit>,
): CompletableFuture<CastResult> {
    val result = CompletableFuture<CastResult>()
    val started = try {
        startup()
    } catch (throwable: Throwable) {
        SkillCastCoordinator.rollbackConsumption(consumption, throwable).completeInto(result)
        return result
    }
    result.whenComplete { _, _ ->
        if (result.isCancelled) started.cancel(false)
    }
    started.whenComplete { _, throwable ->
        if (throwable == null) {
            SkillCastCoordinator.commitConsumption(consumption).whenComplete { committed, commitFailure ->
                if (commitFailure == null) {
                    result.complete(committed)
                } else {
                    SkillCastCoordinator.rollbackConsumption(consumption, commitFailure).completeInto(result)
                }
            }
        } else {
            SkillCastCoordinator.rollbackConsumption(consumption, throwable).completeInto(result)
        }
    }
    return result
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
    return getSkill(skill, create).thenApplyMain {
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
    return getSkill(job, skill, create).thenApplyMain {
        it?.let { it1 -> function(it1) }
    }
}

inline fun <T> Player.skill(
    job: String,
    skill: String,
    create: Boolean = false,
    crossinline function: (IPlayerSkill) -> T
): CompletableFuture<T?> {
    return getSkill(job, skill, create).thenApplyMain {
        it?.let { it1 -> function(it1) }
    }
}

fun Player.getSkill(skill: String, create: Boolean = false): CompletableFuture<IPlayerSkill?> {
    return job().thenComposeMain { currentJob ->
        currentJob?.let { getSkill(it.key, skill, create) }
            ?: CompletableFuture.completedFuture(null)
    }
}

fun Player.getSkill(job: String, skill: String, create: Boolean = false): CompletableFuture<IPlayerSkill?> {
    val skillLoader = SkillLoaderManager.getSkillLoader(skill) ?: return CompletableFuture.completedFuture(null)
    return orryxProfile { profile ->
        MemoryCache.getPlayerSkill(uniqueId, profile.id, job, skill).thenComposeMain { cached ->
            if (cached != null || !create) return@thenComposeMain CompletableFuture.completedFuture(cached)
            val tag = playerJobSkillDataTag(uniqueId, profile.id, job, skill)
            val creation = pendingSkillCreations.computeIfAbsent(tag) {
                val created = PlayerSkill(profile.id, uniqueId, skill, job, skillLoader.minLevel, skillLoader.isLocked)
                MemoryCache.savePlayerSkill(created)
                PersistenceManager.saveSkill(created.createPO(), invalidate = false).thenApply<IPlayerSkill?> {
                    created
                }.whenComplete { _, throwable ->
                    if (throwable != null) MemoryCache.removePlayerSkill(uniqueId, profile.id, job, skill)
                }
            }
            creation.whenComplete { _, _ -> pendingSkillCreations.remove(tag, creation) }
            creation
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

fun ICastSkill.consume(player: Player, parameter: SkillParameter): CompletableFuture<CastResult> {
    return SkillCastCoordinator.consume(player, parameter)
}

internal fun ICastSkill.consumeForStartup(
    player: Player,
    parameter: SkillParameter,
): CompletableFuture<StartupConsumption> {
    return SkillCastCoordinator.consumeForStartup(player, parameter)
}

internal fun ISkill.castSkillRawAsync(
    player: Player,
    parameter: SkillParameter,
    consume: Boolean,
): CompletableFuture<CastResult> {
    return SkillCasterRegistry.getCaster(this)?.cast(this, player, parameter, consume)
        ?: CompletableFuture.completedFuture(CastResult.PARAMETER)
}

fun ISkill.castSkillAsync(player: Player, parameter: SkillParameter, consume: Boolean = true): CompletableFuture<CastResult> {
    debug { "玩家 ${player.name} cast skill $key" }
    return if (consume) {
        SkillCastCoordinator.enqueue(player.uniqueId) {
            castSkillRawAsync(player, parameter, true)
        }
    } else {
        castSkillRawAsync(player, parameter, false)
    }
}

fun ISkill.castSkill(player: Player, parameter: SkillParameter, consume: Boolean = true) {
    castSkillAsync(player, parameter, consume).exceptionally {
        it.printStackTrace()
        null
    }
}

fun IPlayerSkill.tryCast(trigger: SkillTrigger = SkillTrigger.Unknown): CompletableFuture<CastResult> {
    debug { "玩家 ${player.name} try cast skill $key" }
    val parameter = parameter().apply { this.trigger = trigger }
    return SkillCastCoordinator.enqueue(player.uniqueId) {
        castCheck(parameter).thenComposeMain { checkResult ->
            if (!checkResult.isSuccess()) {
                CompletableFuture.completedFuture(checkResult)
            } else if (this is PlayerSkill) {
                castWithinTransactionAsync(parameter, true)
            } else {
                CompletableFuture.completedFuture(cast(parameter, true))
            }
        }
    }.thenApplyMain { result ->
        debug { "玩家 ${player.name} try cast skill result ${result.name}" }
        if (!silence) result.sendLang(player)
        result
    }
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
    return silence(skillParameter, player, skillParameter.silenceValue(true))
}

internal fun ICastSkill.silence(skillParameter: SkillParameter, player: Player, timeout: Long): Long {
    val application = applySilence(skillParameter, player, timeout)
    try {
        commitSilenceState(player, skillParameter, application)
    } catch (throwable: Throwable) {
        application.timedStatus?.let { receipt ->
            ProfileAPI.restoreSilenceTransaction(player, receipt)
        }
        throw throwable
    }
    return application.timeout
}

internal fun ICastSkill.applySilence(
    skillParameter: SkillParameter,
    player: Player,
    timeout: Long,
): SkillSilenceApplication {
    val data = player.statusData()
    if (timeout >= 0) {
        val configured = StateManager.getGlobalState(key) as? SkillState
            ?: error("技能 $key 缺少对应的 SkillState")
        val state = SkillState.Running(data, configured, timeout / 50)
        val event = OrryxPlayerStateSkillEvents.Pre(player, skillParameter, timeout, state)
        if (event.call()) {
            event.state.duration = event.silence.coerceAtLeast(0L) / 50L
            val timedStatus = ProfileAPI.applySilenceTransaction(player, event.silence)
            return SkillSilenceApplication(event.silence, event.state, timedStatus)
        }
        return SkillSilenceApplication(event.silence, null, null)
    }
    return SkillSilenceApplication(0L, null, ProfileAPI.cancelSilenceTransaction(player))
}

internal fun commitSilenceState(
    player: Player,
    skillParameter: SkillParameter,
    application: SkillSilenceApplication,
) {
    val state = application.plannedState ?: return
    player.statusData().commitPreparedState(state)
    runCatching {
        OrryxPlayerStateSkillEvents.Post(player, skillParameter, application.timeout, state).call()
    }.onFailure(Throwable::printStackTrace)
}