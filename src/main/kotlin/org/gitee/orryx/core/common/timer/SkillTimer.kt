package org.gitee.orryx.core.common.timer

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCooldownEvents
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.utils.getSkill
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.util.unsafeLazy
import java.util.*

object SkillTimer : ITimer {

    private val playerCooldowns: MutableMap<UUID, MutableMap<String, CooldownEntry>> by unsafeLazy { hashMapOf() }

    fun reset(player: Player, parameter: SkillParameter) {
        reset(adaptPlayer(player), parameter)
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
        val player = sender.castSafely<Player>() ?: return
        player.getSkill(tag).thenApply {
            val event = OrryxPlayerSkillCooldownEvents.Increase.Pre(player, it ?: return@thenApply, amount)
            if (event.call()) {
                getCooldownMap(sender)[tag]?.addDuration(event.amount)
                OrryxPlayerSkillCooldownEvents.Increase.Post(event.player, event.skill, event.amount).call()
            }
        }
    }

    override fun reduce(sender: ProxyCommandSender, tag: String, amount: Long) {
        val player = sender.castSafely<Player>() ?: return
        player.getSkill(tag).thenApply {
            val event = OrryxPlayerSkillCooldownEvents.Reduce.Pre(player, it ?: return@thenApply, amount)
            if (event.call()) {
                getCooldownMap(sender)[tag]?.reduceDuration(event.amount)
                OrryxPlayerSkillCooldownEvents.Reduce.Post(event.player, event.skill, event.amount).call()
            }
        }
    }

    override fun set(sender: ProxyCommandSender, tag: String, amount: Long) {
        val player = sender.castSafely<Player>() ?: return
        player.getSkill(tag).thenApply {
            val event = OrryxPlayerSkillCooldownEvents.Set.Pre(player, it ?: return@thenApply, amount)
            if (event.call()) {
                val cooldownMap = getCooldownMap(sender)
                cooldownMap.values.removeIf { it.isReady }
                if (event.amount <= 0L) {
                    cooldownMap.remove(tag)
                } else {
                    cooldownMap[tag] = CooldownEntry(tag, event.amount)
                }
                OrryxPlayerSkillCooldownEvents.Set.Post(event.player, event.skill, event.amount).call()
            }
        }
    }

    override fun getCooldownMap(sender: ProxyCommandSender): MutableMap<String, CooldownEntry> {
        val playerId = sender.castSafely<Player>()?.uniqueId ?: throw IllegalArgumentException("Sender must be a Player")
        return playerCooldowns.getOrPut(playerId) { hashMapOf() }
    }

    override fun getCooldownEntry(sender: ProxyCommandSender, tag: String): CooldownEntry? {
        return getCooldownMap(sender)[tag]
    }

    @SubscribeEvent
    private fun onPlayerQuit(e: PlayerQuitEvent) {
        playerCooldowns.remove(e.player.uniqueId)
    }
}