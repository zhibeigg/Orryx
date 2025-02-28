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
import org.gitee.orryx.core.key.BindKeyLoaderManager.getBindKey
import org.gitee.orryx.core.ui.ISkillHud
import org.gitee.orryx.core.ui.ISkillUI
import org.gitee.orryx.core.ui.IUIManager
import org.gitee.orryx.core.ui.bukkit.BukkitSkillHud.Companion.bukkitSkillHudMap
import org.gitee.orryx.core.ui.bukkit.BukkitSkillHud.Companion.getViewerHud
import org.gitee.orryx.core.ui.bukkit.BukkitSkillHud.Companion.skillCooldownMap
import org.gitee.orryx.core.ui.bukkit.BukkitSkillHud.Companion.slotIndex
import org.gitee.orryx.core.ui.bukkit.BukkitSkillHud.Companion.slots
import org.gitee.orryx.utils.loadFromFile
import org.gitee.orryx.utils.tryCast
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.submit
import taboolib.module.configuration.Configuration
import taboolib.module.database.Database

class BukkitUIManager: IUIManager {

    init {
        val task = submit(period = 5) {
            bukkitSkillHudMap.forEach {
                it.value.forEach { map ->
                    map.value.update()
                }
            }
        }

        Database.prepareClose {
            task.cancel()
        }

        registerBukkitListener(PlayerItemHeldEvent::class.java, EventPriority.MONITOR) { e ->
            if (e.isCancelled) return@registerBukkitListener
            val bindKey = getBindKey("MC" + (e.newSlot + 1)) ?: return@registerBukkitListener
            val ui = getViewerHud(e.player) ?: return@registerBukkitListener
            if (ui.viewer == ui.owner || ui.viewer.isOp) {
                bindKey.tryCast(ui.owner)
                e.isCancelled = true
            }
        }

        registerBukkitListener(OrryxPlayerSkillCooldownEvents.Set::class.java, EventPriority.MONITOR) { e ->
            if (e.isCancelled) return@registerBukkitListener
            val cooldownMap = skillCooldownMap.getOrPut(e.player.uniqueId) { mutableMapOf() }
            val iterator = cooldownMap.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.isOver(e.player)) {
                    iterator.remove()
                }
            }
            cooldownMap[e.skill.key] = BukkitSkillHud.Companion.Cooldown(e.skill.key, e.amount).apply { update(e.player) }
        }

        registerBukkitListener(OrryxPlayerSkillCooldownEvents.Increase::class.java, EventPriority.MONITOR) { e ->
            if (e.isCancelled) return@registerBukkitListener
            val cooldownMap = skillCooldownMap.getOrPut(e.player.uniqueId) { mutableMapOf() }
            cooldownMap[e.skill.key]?.update(e.player)
        }

        registerBukkitListener(OrryxPlayerSkillCooldownEvents.Reduce::class.java, EventPriority.MONITOR) { e ->
            if (e.isCancelled) return@registerBukkitListener
            val cooldownMap = skillCooldownMap.getOrPut(e.player.uniqueId) { mutableMapOf() }
            cooldownMap[e.skill.key]?.update(e.player)
        }

        registerBukkitListener(OrryxPlayerSkillBindKeyEvent::class.java, EventPriority.MONITOR) { e ->
            if (e.isCancelled) return@registerBukkitListener
            bukkitSkillHudMap[e.player.uniqueId]?.forEach {
                it.value.update()
            }
        }

        registerBukkitListener(OrryxPlayerSkillUnBindKeyEvent::class.java, EventPriority.MONITOR) { e ->
            if (e.isCancelled) return@registerBukkitListener
            bukkitSkillHudMap[e.player.uniqueId]?.forEach {
                it.value.update()
            }
        }

        registerBukkitListener(PlayerQuitEvent::class.java) { e ->
            bukkitSkillHudMap.remove(e.player.uniqueId)?.forEach {
                it.value.close()
            }
            skillCooldownMap.remove(e.player.uniqueId)
        }

        registerBukkitListener(InventoryClickEvent::class.java) { e ->
            if (e.isCancelled) return@registerBukkitListener
            when(e.click) {
                DOUBLE_CLICK, LEFT, RIGHT, MIDDLE, WINDOW_BORDER_LEFT, SHIFT_LEFT, SHIFT_RIGHT, WINDOW_BORDER_RIGHT, CREATIVE, DROP, CONTROL_DROP, SWAP_OFFHAND, UNKNOWN -> {
                    if (e.rawSlot in slots) {
                        getBindKey("MC" + (slots.indexOf(e.rawSlot) + 1)) ?: return@registerBukkitListener
                        if (bukkitSkillHudMap.any { it.value.containsKey(e.view.player.uniqueId) }) {
                            e.isCancelled = true
                        }
                    }
                }
                NUMBER_KEY -> {
                    if (e.hotbarButton in slotIndex) {
                        getBindKey("MC" + (e.hotbarButton + 1)) ?: return@registerBukkitListener
                        if (bukkitSkillHudMap.any { it.value.containsKey(e.view.player.uniqueId) }) {
                            e.isCancelled = true
                        }
                    }
                }
            }
        }

        registerBukkitListener(InventoryDragEvent::class.java) { e ->
            if (e.isCancelled) return@registerBukkitListener
            if (e.rawSlots.any { it in slots }) {
                if (e.rawSlots.any { getBindKey("MC" + (slots.indexOf(it) + 1)) != null }) {
                    if (bukkitSkillHudMap.any { it.value.containsKey(e.view.player.uniqueId) }) {
                        e.isCancelled = true
                    }
                }
            }
        }

        registerBukkitListener(PlayerJoinEvent::class.java) { e ->
            e.player.inventory.heldItemSlot = slotIndex.firstOrNull { getBindKey("MC" + (it + 1)) == null } ?: 0
            BukkitSkillHud(e.player, e.player).open()
        }

    }

    override val config: Configuration = loadFromFile("ui/bukkit/setting.yml")

    override fun createSkillUI(viewer: Player, owner: Player): ISkillUI {
        return BukkitSkillUI(viewer, owner)
    }

    override fun createSkillHUD(viewer: Player, owner: Player): ISkillHud {
        return BukkitSkillHud(viewer, owner)
    }

    override fun getSkillHUD(viewer: Player): ISkillHud? {
        return getViewerHud(viewer)
    }

}