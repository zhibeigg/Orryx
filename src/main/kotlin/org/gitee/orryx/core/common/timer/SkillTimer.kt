package org.gitee.orryx.core.common.timer

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.common.CooldownEntry
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.adaptPlayer
import java.util.*

object SkillTimer : ITimer {

    private val playerCooldowns = mutableMapOf<UUID, MutableMap<String, CooldownEntry>>()

    fun reset(player: Player, parameter: SkillParameter) {
        reset(adaptPlayer(player), parameter)
    }

    override fun reset(sender: ProxyCommandSender, parameter: IParameter): Long {
        require(parameter is SkillParameter) { "Invalid parameter type" }
        val timeout = parameter.cooldownValue(true)
        val skillName = parameter.skill ?: return timeout

        if (timeout > 0) {
            set(sender, skillName, timeout)
        } else {
            getCooldownMap(sender).remove(skillName)
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
        return getCooldownMap(sender)[tag]?.remaining ?: 0
    }

    fun increase(player: Player, skill: String, amount: Long) {
        increase(adaptPlayer(player), skill, amount)
    }

    override fun increase(sender: ProxyCommandSender, tag: String, amount: Long) {
        getCooldownMap(sender)[tag]?.addDuration(amount)
    }

    fun reduce(player: Player, skill: String, amount: Long) {
        reduce(adaptPlayer(player), skill, amount)
    }

    override fun reduce(sender: ProxyCommandSender, tag: String, amount: Long) {
        getCooldownMap(sender)[tag]?.reduceDuration(amount)
    }

    fun set(player: Player, skill: String, amount: Long) {
        set(adaptPlayer(player), skill, amount)
    }

    override fun set(sender: ProxyCommandSender, tag: String, amount: Long) {
        val cooldownMap = getCooldownMap(sender)
        cooldownMap.values.removeIf { it.isReady }
        cooldownMap[tag] = CooldownEntry(tag, amount)
    }

    override fun getCooldownMap(sender: ProxyCommandSender): MutableMap<String, CooldownEntry> {
        val playerId = sender.castSafely<Player>()?.uniqueId ?: throw IllegalArgumentException("Sender must be a Player")
        return playerCooldowns.computeIfAbsent(playerId) { mutableMapOf() }
    }

    @SubscribeEvent
    private fun onPlayerQuit(e: PlayerQuitEvent) {
        playerCooldowns.remove(e.player.uniqueId)
    }

}