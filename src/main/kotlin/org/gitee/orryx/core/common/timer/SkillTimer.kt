package org.gitee.orryx.core.common.timer

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCooldownEvents
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.utils.getSkill
import org.gitee.orryx.utils.thenApplyMain
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.adaptPlayer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal class CooldownApplication(
    val previousExpiresAt: Long?,
    val appliedVersion: Long?,
    val skill: IPlayerSkill?,
    val amount: Long,
) {
    val committed = AtomicBoolean(false)
}

object SkillTimer : ITimer {

    private val playerCooldowns = ConcurrentHashMap<UUID, ConcurrentHashMap<String, CooldownEntry>>()
    private val cooldownVersions = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()
    private val versionSequence = AtomicLong()

    fun reset(player: Player, parameter: SkillParameter) {
        reset(adaptPlayer(player), parameter)
    }

    /** 已持有玩家技能实例时，在当前主线程原子设置冷却并同步派发事件。 */
    fun reset(skill: IPlayerSkill, parameter: SkillParameter): Long {
        return reset(skill, parameter.cooldownValue(true))
    }

    fun resetAsync(skill: IPlayerSkill, parameter: SkillParameter): java.util.concurrent.CompletableFuture<Long> {
        return parameter.cooldownValueFuture(true).thenApplyMain { timeout -> reset(skill, timeout) }
    }

    internal fun reset(skill: IPlayerSkill, timeout: Long): Long {
        commit(apply(skill, timeout))
        return getCountdown(skill.player, skill.key)
    }

    internal fun apply(skill: IPlayerSkill, timeout: Long): CooldownApplication {
        val player = skill.player
        val map = getCooldownMap(adaptPlayer(player))
        val previous = map[skill.key]?.overStamp?.takeIf { it > System.currentTimeMillis() }
        val event = OrryxPlayerSkillCooldownEvents.Set.Pre(player, skill, timeout.coerceAtLeast(0L))
        if (!event.call()) return CooldownApplication(previous, null, null, getCountdown(player, skill.key))

        val applied = event.amount.coerceAtLeast(0L)
        map.values.removeIf { entry -> entry.isReady }
        if (applied == 0L) map.remove(skill.key) else map[skill.key] = CooldownEntry(skill.key, applied)
        val version = markVersion(player.uniqueId, skill.key)
        return CooldownApplication(previous, version, skill, applied)
    }

    internal fun commit(application: CooldownApplication) {
        val skill = application.skill ?: return
        if (!application.committed.compareAndSet(false, true)) return
        runCatching {
            OrryxPlayerSkillCooldownEvents.Set.Post(skill.player, skill, application.amount).call()
        }.onFailure(Throwable::printStackTrace)
    }

    override fun reset(sender: ProxyCommandSender, parameter: IParameter): Long {
        require(parameter is SkillParameter) { "Invalid parameter type" }
        val timeout = parameter.cooldownValue(true)
        val skillKey = parameter.skill ?: return timeout

        if (timeout >= 0) {
            set(sender, skillKey, timeout)
        } else {
            set(sender, skillKey, 0)
        }
        return timeout
    }

    fun hasNext(player: Player, skill: String): Boolean {
        return hasNext(adaptPlayer(player), skill)
    }

    internal fun restore(player: Player, skill: String, application: CooldownApplication) {
        val appliedVersion = application.appliedVersion ?: return
        if (cooldownVersions[player.uniqueId]?.get(skill) != appliedVersion) return
        val map = getCooldownMap(adaptPlayer(player))
        val now = System.currentTimeMillis()
        val remaining = application.previousExpiresAt?.let { CooldownEntry.positiveDifference(it, now) } ?: 0L
        if (remaining <= 0L) map.remove(skill) else map[skill] = CooldownEntry(skill, remaining)
        markVersion(player.uniqueId, skill)
    }

    override fun hasNext(sender: ProxyCommandSender, tag: String): Boolean {
        return getCooldownMap(sender)[tag]?.isReady ?: true
    }

    fun getCountdown(player: Player, skill: String): Long {
        return getCountdown(adaptPlayer(player), skill)
    }

    override fun getCountdown(sender: ProxyCommandSender, tag: String): Long {
        return getCooldownMap(sender)[tag]?.countdown ?: 0
    }

    override fun increase(sender: ProxyCommandSender, tag: String, amount: Long) {
        increaseAsync(sender, tag, amount).exceptionally { it.printStackTrace(); null }
    }

    fun increaseAsync(sender: ProxyCommandSender, tag: String, amount: Long): java.util.concurrent.CompletableFuture<Unit> {
        val player = sender.castSafely<Player>() ?: return java.util.concurrent.CompletableFuture.completedFuture(Unit)
        return player.getSkill(tag).thenApplyMain { skill ->
            val event = OrryxPlayerSkillCooldownEvents.Increase.Pre(player, skill ?: return@thenApplyMain Unit, amount)
            if (event.call()) {
                getCooldownMap(sender).let { map ->
                    map[tag]?.addDuration(event.amount) ?: run {
                        map[tag] = CooldownEntry(tag, event.amount)
                    }
                }
                markVersion(player.uniqueId, tag)
                OrryxPlayerSkillCooldownEvents.Increase.Post(event.player, event.skill, event.amount).call()
            }
            Unit
        }
    }

    override fun reduce(sender: ProxyCommandSender, tag: String, amount: Long) {
        reduceAsync(sender, tag, amount).exceptionally { it.printStackTrace(); null }
    }

    fun reduceAsync(sender: ProxyCommandSender, tag: String, amount: Long): java.util.concurrent.CompletableFuture<Unit> {
        val player = sender.castSafely<Player>() ?: return java.util.concurrent.CompletableFuture.completedFuture(Unit)
        return player.getSkill(tag).thenApplyMain { skill ->
            val event = OrryxPlayerSkillCooldownEvents.Reduce.Pre(player, skill ?: return@thenApplyMain Unit, amount)
            if (event.call()) {
                getCooldownMap(sender)[tag]?.reduceDuration(event.amount)
                markVersion(player.uniqueId, tag)
                OrryxPlayerSkillCooldownEvents.Reduce.Post(event.player, event.skill, event.amount).call()
            }
            Unit
        }
    }

    override fun set(sender: ProxyCommandSender, tag: String, amount: Long) {
        setAsync(sender, tag, amount).exceptionally { it.printStackTrace(); null }
    }

    fun setAsync(sender: ProxyCommandSender, tag: String, amount: Long): java.util.concurrent.CompletableFuture<Unit> {
        val player = sender.castSafely<Player>() ?: return java.util.concurrent.CompletableFuture.completedFuture(Unit)
        return player.getSkill(tag).thenApplyMain { skill ->
            val event = OrryxPlayerSkillCooldownEvents.Set.Pre(player, skill ?: return@thenApplyMain Unit, amount)
            if (event.call()) {
                val cooldownMap = getCooldownMap(sender)
                cooldownMap.values.removeIf { c -> c.isReady }
                if (event.amount <= 0L) {
                    cooldownMap.remove(tag)
                } else {
                    cooldownMap[tag] = CooldownEntry(tag, event.amount)
                }
                markVersion(player.uniqueId, tag)
                OrryxPlayerSkillCooldownEvents.Set.Post(event.player, event.skill, event.amount).call()
            }
            Unit
        }
    }

    override fun getCooldownMap(sender: ProxyCommandSender): MutableMap<String, CooldownEntry> {
        val playerId = sender.castSafely<Player>()?.uniqueId ?: throw IllegalArgumentException("Sender must be a Player")
        return playerCooldowns.getOrPut(playerId) { ConcurrentHashMap() }
    }

    override fun getCooldownEntry(sender: ProxyCommandSender, tag: String): CooldownEntry? {
        return getCooldownMap(sender)[tag]
    }

    private fun markVersion(player: UUID, skill: String): Long {
        val version = versionSequence.incrementAndGet()
        cooldownVersions.getOrPut(player) { ConcurrentHashMap() }[skill] = version
        return version
    }

    @SubscribeEvent
    private fun onPlayerQuit(e: PlayerQuitEvent) {
        playerCooldowns.remove(e.player.uniqueId)
        cooldownVersions.remove(e.player.uniqueId)
    }
}