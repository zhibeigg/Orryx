package org.gitee.orryx.core.ui.bukkit

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillBindKeyEvent
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCooldownEvents
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillUnBindKeyEvent
import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.key.BindKeyLoaderManager.getBindKey
import org.gitee.orryx.core.ui.AbstractSkillHud
import org.gitee.orryx.utils.getBindSkill
import org.gitee.orryx.utils.getDescriptionComparison
import org.gitee.orryx.utils.getIcon
import org.gitee.orryx.utils.tryCast
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.common5.cshort
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.buildItem
import java.util.*

class BukkitSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    companion object {

        private val slots = (0..8).toList()
        // owner
        private val skillCooldownMap = mutableMapOf<UUID, MutableMap<String, Cooldown>>()
        // owner, viewer to HUD
        private val bukkitSkillHudMap = mutableMapOf<UUID, MutableMap<UUID, BukkitSkillHud>>()

        class Cooldown(val skill: String, val max: Long) {

            fun percent(player: Player): Double {
                return (max.cdouble - SkillTimer.getCountdown(player, skill).cdouble)/max.cdouble
            }

            fun isOver(player: Player): Boolean {
                return SkillTimer.getCountdown(player, skill) <= 0L
            }

            fun update(player: Player) {
                bukkitSkillHudMap[player.uniqueId]?.forEach { (_, u) ->
                    u.update()
                }
            }

        }

        @SubscribeEvent
        private fun click(e: PlayerItemHeldEvent) {
            val bindKey = getBindKey("MC" + (e.newSlot + 1)) ?: return
            bindKey.tryCast(e.player)
            e.isCancelled = true
        }

        @SubscribeEvent(priority = EventPriority.MONITOR)
        private fun cooldown(e: OrryxPlayerSkillCooldownEvents.Set) {
            if (e.isCancelled) return
            val cooldownMap = skillCooldownMap.getOrPut(e.player.uniqueId) { mutableMapOf() }
            val iterator = cooldownMap.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.isOver(e.player)) {
                    iterator.remove()
                }
            }
            cooldownMap[e.skill.key] = Cooldown(e.skill.key, e.amount).apply { update(e.player) }
        }

        @SubscribeEvent(priority = EventPriority.MONITOR)
        private fun cooldown(e: OrryxPlayerSkillCooldownEvents.Increase) {
            if (e.isCancelled) return
            val cooldownMap = skillCooldownMap.getOrPut(e.player.uniqueId) { mutableMapOf() }
            cooldownMap[e.skill.key]?.update(e.player)
        }

        @SubscribeEvent(priority = EventPriority.MONITOR)
        private fun cooldown(e: OrryxPlayerSkillCooldownEvents.Reduce) {
            if (e.isCancelled) return
            val cooldownMap = skillCooldownMap.getOrPut(e.player.uniqueId) { mutableMapOf() }
            cooldownMap[e.skill.key]?.update(e.player)
        }

        @SubscribeEvent(priority = EventPriority.MONITOR)
        private fun bind(e: OrryxPlayerSkillBindKeyEvent) {
            if (e.isCancelled) return
            bukkitSkillHudMap[e.player.uniqueId]?.forEach {
                it.value.update()
            }
        }

        @SubscribeEvent(priority = EventPriority.MONITOR)
        private fun unbind(e: OrryxPlayerSkillUnBindKeyEvent) {
            if (e.isCancelled) return
            bukkitSkillHudMap[e.player.uniqueId]?.forEach {
                it.value.update()
            }
        }

        @SubscribeEvent
        private fun quit(event: PlayerQuitEvent) {
            bukkitSkillHudMap.remove(event.player.uniqueId)?.forEach {
                it.value.close()
            }
            skillCooldownMap.remove(event.player.uniqueId)
        }

        @SubscribeEvent
        private fun click(e: InventoryClickEvent) {
            if (e.rawSlot in slots) {
                getBindKey("MC" + (slots.indexOf(e.rawSlot) + 1)) ?: return
                if (bukkitSkillHudMap.any { it.value.containsKey(e.view.player.uniqueId) }) {
                    e.isCancelled = true
                }
            }
        }

        @SubscribeEvent
        private fun drag(e: InventoryDragEvent) {
            if (e.rawSlots.any { it in slots }) {
                if (e.rawSlots.any { getBindKey("MC" + (slots.indexOf(it) + 1)) != null }) {
                    if (bukkitSkillHudMap.any { it.value.containsKey(e.view.player.uniqueId) }) {
                        e.isCancelled = true
                    }
                }
            }
        }

        @SubscribeEvent
        private fun join(e: PlayerJoinEvent) {
            BukkitSkillHud(e.player, e.player).open()
        }

    }

    override fun open() {
        bukkitSkillHudMap.getOrPut(owner.uniqueId) { mutableMapOf() }[viewer.uniqueId] = this
        update()
    }

    override fun update() {
        slots.forEachIndexed { index, i ->
            val bindKey = getBindKey("MC" + (index + 1)) ?: return@forEachIndexed
            val skill = bindKey.getBindSkill(owner) ?: return@forEachIndexed
            viewer.inventory.setItem(
                i,
                buildItem(XMaterial.matchXMaterial(skill.skill.xMaterial).orElse(XMaterial.BLAZE_ROD)) {
                    name = skill.getIcon()
                    lore += skill.getDescriptionComparison()
                    amount = index + 1
                    finishing = {
                        it.durability = (it.type.maxDurability * (skillCooldownMap[owner.uniqueId]?.get(skill.key)?.percent(owner) ?: 1.0)).cint.cshort
                    }
                    colored()
                }
            )
        }
    }

    fun close() {
        slots.forEachIndexed { index, i ->
            val bindKey = getBindKey("MC" + (index + 1)) ?: return@forEachIndexed
            bindKey.getBindSkill(owner) ?: return@forEachIndexed
            viewer.inventory.setItem(i, null)
        }
    }

}