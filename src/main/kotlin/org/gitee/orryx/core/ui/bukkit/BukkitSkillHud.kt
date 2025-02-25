package org.gitee.orryx.core.ui.bukkit

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType.*
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
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.ui.AbstractSkillHud
import org.gitee.orryx.utils.getBindSkill
import org.gitee.orryx.utils.getDescriptionComparison
import org.gitee.orryx.utils.getIcon
import org.gitee.orryx.utils.tryCast
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.buildItem
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class BukkitSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    companion object {

        private const val TAG = "OrryxHud"
        private val slots = (36..44).toList()
        private val slotIndex = (0..8).toList()

        /**
         * owner, skill, [Cooldown]
         */
        private val skillCooldownMap = mutableMapOf<UUID, MutableMap<String, Cooldown>>()
        /**
         * owner, viewer, [BukkitSkillHud]
         */
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

        fun getViewerHud(player: Player): BukkitSkillHud? {
            return bukkitSkillHudMap.firstNotNullOfOrNull {
                it.value[player.uniqueId]
            }
        }

        @Schedule(async = false, period = 5)
        private fun cooldown() {
            bukkitSkillHudMap.forEach {
                it.value.forEach { map ->
                    map.value.update()
                }
            }
        }

        @SubscribeEvent
        private fun click(e: PlayerItemHeldEvent) {
            val bindKey = getBindKey("MC" + (e.newSlot + 1)) ?: return
            val ui = getViewerHud(e.player) ?: return
            if (ui.viewer == ui.owner || ui.viewer.isOp) {
                bindKey.tryCast(ui.owner)
                e.isCancelled = true
            }
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
            when(e.click) {
                DOUBLE_CLICK, LEFT, RIGHT, MIDDLE, WINDOW_BORDER_LEFT, SHIFT_LEFT, SHIFT_RIGHT, WINDOW_BORDER_RIGHT, CREATIVE, DROP, CONTROL_DROP, SWAP_OFFHAND, UNKNOWN -> {
                    if (e.rawSlot in slots) {
                        getBindKey("MC" + (slots.indexOf(e.rawSlot) + 1)) ?: return
                        if (bukkitSkillHudMap.any { it.value.containsKey(e.view.player.uniqueId) }) {
                            e.isCancelled = true
                        }
                    }
                }
                NUMBER_KEY -> {
                    if (e.hotbarButton in slotIndex) {
                        getBindKey("MC" + (e.hotbarButton + 1)) ?: return
                        if (bukkitSkillHudMap.any { it.value.containsKey(e.view.player.uniqueId) }) {
                            e.isCancelled = true
                        }
                    }
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
            e.player.inventory.heldItemSlot = slotIndex.firstOrNull { getBindKey("MC" + (it + 1)) == null } ?: 0
            BukkitSkillHud(e.player, e.player).open()
        }

        @Reload(2)
        private fun reload() {
            bukkitSkillHudMap.forEach {
                it.value.forEach { map ->
                    map.value.update()
                }
            }
        }

    }

    override fun open() {
        remove()
        bukkitSkillHudMap.getOrPut(owner.uniqueId) { mutableMapOf() }[viewer.uniqueId] = this
        update()
    }

    override fun update() {
        slotIndex.forEach { i ->
            val bindKey = getBindKey("MC" + (i + 1)) ?: return@forEach
            val skill = bindKey.getBindSkill(owner) ?: run {
                viewer.inventory.setItem(
                    i,
                    buildItem(XMaterial.BARRIER) {
                        name = "&f技能槽"
                        lore += "&f无技能绑定的技能槽位"
                        amount = i + 1
                        hideAll()
                        unique()
                        colored()
                    }
                )
                return@forEach
            }
            val material = XMaterial.matchXMaterial(skill.skill.xMaterial).orElse(XMaterial.BLAZE_ROD)
            viewer.inventory.setItem(
                i,
                buildItem(material) {
                    name = skill.getIcon()
                    lore += skill.getDescriptionComparison()
                    amount = i + 1
                    hideAll()
                    unique()
                    material.parseMaterial()?.maxDurability?.let {
                        val percent = skillCooldownMap[owner.uniqueId]?.get(skill.key)?.percent(owner) ?: 1.0
                        if (percent < 1) {
                            damage = (it.cdouble * percent).cint
                        }
                    }
                    colored()
                }
            )
        }
    }

    override fun close() {
        remove()
        slotIndex.forEach { i ->
            getBindKey("MC" + (i + 1)) ?: return@forEach
            viewer.inventory.setItem(i, null)
        }
    }

    fun remove() {
        bukkitSkillHudMap.forEach {
            val iterator = it.value.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next.key == viewer.uniqueId) {
                    iterator.remove()
                }
            }
        }
    }

}