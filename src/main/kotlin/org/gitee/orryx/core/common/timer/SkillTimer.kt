package org.gitee.orryx.core.common.timer

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.common.Baffle
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.adaptPlayer
import java.util.*

object SkillTimer: ITimer {

    private val map = mutableMapOf<UUID, MutableMap<String, Baffle>>()

    fun reset(player: Player, parameter: SkillParameter) {
        reset(adaptPlayer(player), parameter)
    }

    override fun reset(sender: ProxyCommandSender, parameter: IParameter): Long {
        parameter as SkillParameter
        val timeout = parameter.cooldownValue(true)
        if (timeout > 0) {
            set(sender, parameter.skill ?: return timeout, timeout)
        } else {
            getCache(sender).remove(parameter.skill)
        }
        return timeout
    }

    fun hasNext(player: Player, skill: String): Boolean {
        return hasNext(adaptPlayer(player), skill)
    }

    override fun hasNext(sender: ProxyCommandSender, tag: String): Boolean {
        return getCache(sender)[tag]?.next ?: true
    }

    fun getCountdown(player: Player, skill: String): Long {
        return getCountdown(player, skill)
    }

    override fun getCountdown(sender: ProxyCommandSender, tag: String): Long {
        return getCache(sender)[tag]?.countdown ?: 0
    }

    fun increase(player: Player, skill: String, amount: Long) {
        increase(adaptPlayer(player), skill, amount)
    }

    override fun increase(sender: ProxyCommandSender, tag: String, amount: Long) {
        val baffle = getCache(sender)[tag] ?: return
        baffle.increase(amount)
    }

    fun reduce(player: Player, skill: String, amount: Long) {
        reduce(adaptPlayer(player), skill, amount)
    }

    override fun reduce(sender: ProxyCommandSender, tag: String, amount: Long) {
        val baffle = getCache(sender)[tag] ?: return
        baffle.reduce(amount)
    }

    fun set(player: Player, skill: String, amount: Long) {
        set(adaptPlayer(player), skill, amount)
    }

    override fun set(sender: ProxyCommandSender, tag: String, amount: Long) {
        val baffles = getCache(sender)
        val iterator = baffles.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val value = entry.value
            if (value.next) {
                iterator.remove()
            }
        }
        baffles[tag] = Baffle(tag, amount)
    }

    override fun getCache(sender: ProxyCommandSender) = map.computeIfAbsent(sender.cast<Player>().uniqueId) { mutableMapOf() }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        map.remove(e.player.uniqueId)
    }

}