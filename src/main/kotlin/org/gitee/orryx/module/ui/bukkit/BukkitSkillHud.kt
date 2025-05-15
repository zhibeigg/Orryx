package org.gitee.orryx.module.ui.bukkit

import org.bukkit.entity.Player
import org.gitee.orryx.core.key.BindKeyLoaderManager.getBindKey
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.module.ui.AbstractSkillHud
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.module.ui.IUIManager.Companion.skillCooldownMap
import org.gitee.orryx.utils.getBindSkill
import org.gitee.orryx.utils.getDescriptionComparison
import org.gitee.orryx.utils.getIcon
import taboolib.common.util.unsafeLazy
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.buildItem
import java.util.*
import kotlin.collections.set

open class BukkitSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    companion object {

        internal const val TAG = "OrryxHud"
        internal val slots = (36..44).toList()
        internal val slotIndex = (0..8).toList()

        /**
         * owner, viewer, [BukkitSkillHud]
         */
        internal val bukkitSkillHudMap by unsafeLazy { hashMapOf<UUID, MutableMap<UUID, BukkitSkillHud>>() }

        fun getViewerHud(player: Player): BukkitSkillHud? {
            return bukkitSkillHudMap.firstNotNullOfOrNull {
                it.value[player.uniqueId]
            }
        }

        @Reload(2)
        private fun reload() {
            if (IUIManager.INSTANCE !is BukkitUIManager) return
            bukkitSkillHudMap.forEach {
                it.value.forEach { map ->
                    map.value.update()
                }
            }
        }
    }

    override fun open() {
        remove()
        viewer.inventory.heldItemSlot = slotIndex.firstOrNull { getBindKey("MC" + (it + 1)) == null } ?: 0
        bukkitSkillHudMap.getOrPut(owner.uniqueId) { hashMapOf() }[viewer.uniqueId] = this
        update()
    }

    override fun update(skill: IPlayerSkill?) {
        if (skill != null) {
            slotIndex.forEach { i ->
                val bindKey = getBindKey("MC" + (i + 1)) ?: return@forEach
                bindKey.getBindSkill(owner).thenAccept { s ->
                    if (s != skill) return@thenAccept
                    val material = XMaterial.matchXMaterial(skill.skill.xMaterial).orElse(XMaterial.BLAZE_ROD)
                    viewer.inventory.setItem(
                        i,
                        buildItem(material) {
                            name = skill.getIcon()
                            lore += skill.getDescriptionComparison()
                            amount = i + 1
                            hideAll()
                            unique()
                            material.get()?.maxDurability?.let {
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
        } else {
            slotIndex.forEach { i ->
                val bindKey = getBindKey("MC" + (i + 1)) ?: return@forEach
                bindKey.getBindSkill(owner).thenAccept { skill ->
                    skill ?: run {
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
                        return@thenAccept
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
                            material.get()?.maxDurability?.let {
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
        }
    }

    override fun close() {
        remove()
        slotIndex.forEach { i ->
            getBindKey("MC" + (i + 1)) ?: return@forEach
            viewer.inventory.setItem(i, null)
        }
    }

    protected open fun remove() {
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