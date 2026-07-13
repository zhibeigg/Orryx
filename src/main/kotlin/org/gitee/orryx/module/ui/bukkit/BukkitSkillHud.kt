package org.gitee.orryx.module.ui.bukkit

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.gitee.orryx.core.key.BindKeyLoaderManager.getBindKey
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.module.ui.AbstractSkillHud
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.module.ui.IUIManager.Companion.skillCooldownMap
import org.gitee.orryx.utils.getBindSkill
import org.gitee.orryx.utils.getDescriptionComparison
import org.gitee.orryx.utils.getIcon
import taboolib.common.function.debounce
import taboolib.common.platform.function.submit
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.buildItem
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

open class BukkitSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    private data class SlotBinding(val active: Boolean, val skill: IPlayerSkill?)

    private data class SlotSnapshot(
        val skillKey: String?,
        val material: XMaterial,
        val name: String,
        val lore: List<String>,
        val amount: Int,
        val damage: Int?
    )

    private val pendingFullRefresh = AtomicBoolean()
    private val pendingSkillKeys = ConcurrentHashMap.newKeySet<String>()
    private val debouncedUpdate = debounce(50L) { _: Unit ->
        submit { applyPendingUpdates() }
    }

    private val slotSkills = arrayOfNulls<IPlayerSkill>(slotIndex.size)
    private val activeSlots = BooleanArray(slotIndex.size)
    private val dirtySlots = BooleanArray(slotIndex.size) { true }
    private val snapshots = arrayOfNulls<SlotSnapshot>(slotIndex.size)
    private val renderedItems = arrayOfNulls<ItemStack>(slotIndex.size)

    private var bindingsDirty = true
    private var bindingsLoading = false
    private var bindingsLoaded = false
    private var active = false

    companion object {

        internal val slots = (36..44).toList()
        internal val slotIndex = (0..8).toList()

        /**
         * owner, viewer, [BukkitSkillHud]
         */
        internal val bukkitSkillHudMap = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, BukkitSkillHud>>()
        internal val bukkitSkillHudByViewer = ConcurrentHashMap<UUID, BukkitSkillHud>()

        fun getViewerHud(player: Player): BukkitSkillHud? {
            return bukkitSkillHudByViewer[player.uniqueId]
        }

        internal fun hasViewerHud(viewer: UUID): Boolean {
            return bukkitSkillHudByViewer.containsKey(viewer)
        }

        @Reload(2)
        private fun reload() {
            if (IUIManager.INSTANCE !is BukkitUIManager) return
            bukkitSkillHudByViewer.values.forEach { it.update() }
        }
    }

    override fun open() {
        remove()
        active = true
        viewer.inventory.heldItemSlot = slotIndex.firstOrNull { getBindKey("MC" + (it + 1)) == null } ?: 0
        bukkitSkillHudMap.getOrPut(owner.uniqueId) { ConcurrentHashMap() }[viewer.uniqueId] = this
        bukkitSkillHudByViewer[viewer.uniqueId] = this
        update()
    }

    override fun update(skill: IPlayerSkill?) {
        if (skill == null) {
            pendingFullRefresh.set(true)
        } else {
            pendingSkillKeys += skill.key
        }
        debouncedUpdate(Unit)
    }

    internal fun refreshTick() {
        if (!active) return
        applyPendingUpdates()
        if (bindingsDirty || !bindingsLoaded) {
            refreshBindings()
        }
        if (bindingsLoaded) {
            refreshSlots(onlyDirty = false)
        }
    }

    private fun applyPendingUpdates() {
        if (!active) return
        if (pendingFullRefresh.getAndSet(false)) {
            pendingSkillKeys.clear()
            bindingsDirty = true
            dirtySlots.fill(true)
            refreshBindings()
            return
        }
        if (pendingSkillKeys.isEmpty()) return
        val skillKeys = pendingSkillKeys.toSet()
        pendingSkillKeys.removeAll(skillKeys)
        if (!bindingsLoaded) {
            bindingsDirty = true
            refreshBindings()
            return
        }
        slotSkills.forEachIndexed { index, skill ->
            if (skill?.key in skillKeys) {
                dirtySlots[index] = true
            }
        }
        refreshSlots(onlyDirty = true)
    }

    private fun refreshBindings() {
        if (!active || bindingsLoading || !bindingsDirty) return
        bindingsLoading = true
        bindingsDirty = false
        val results = arrayOfNulls<SlotBinding>(slotIndex.size)
        val futures = ArrayList<CompletableFuture<*>>(slotIndex.size)
        slotIndex.forEach { index ->
            val bindKey = getBindKey("MC" + (index + 1))
            if (bindKey == null) {
                results[index] = SlotBinding(active = false, skill = null)
            } else {
                futures += bindKey.getBindSkill(owner).whenComplete { skill, _ ->
                    results[index] = SlotBinding(active = true, skill = skill)
                }
            }
        }
        if (futures.isEmpty()) {
            applyBindings(results)
            return
        }
        CompletableFuture.allOf(*futures.toTypedArray()).whenComplete { _, _ ->
            submit {
                if (!active || bukkitSkillHudByViewer[viewer.uniqueId] !== this@BukkitSkillHud) {
                    bindingsLoading = false
                    return@submit
                }
                applyBindings(results)
            }
        }
    }

    private fun applyBindings(results: Array<SlotBinding?>) {
        bindingsLoading = false
        results.forEachIndexed { index, result ->
            result ?: return@forEachIndexed
            activeSlots[index] = result.active
            slotSkills[index] = result.skill
            dirtySlots[index] = result.active
            if (!result.active) {
                snapshots[index] = null
                renderedItems[index] = null
            }
        }
        bindingsLoaded = true
        refreshSlots(onlyDirty = true)
        if (bindingsDirty) {
            refreshBindings()
        }
    }

    private fun refreshSlots(onlyDirty: Boolean) {
        slotIndex.forEach { index ->
            if (!activeSlots[index] || onlyDirty && !dirtySlots[index]) return@forEach
            dirtySlots[index] = false
            applySnapshot(index, createSnapshot(index, slotSkills[index]))
        }
    }

    private fun createSnapshot(index: Int, skill: IPlayerSkill?): SlotSnapshot {
        if (skill == null) {
            return SlotSnapshot(
                skillKey = null,
                material = XMaterial.BARRIER,
                name = "&f技能槽",
                lore = listOf("&f无技能绑定的技能槽位"),
                amount = index + 1,
                damage = null
            )
        }
        val material = XMaterial.matchXMaterial(skill.skill.xMaterial).orElse(XMaterial.BLAZE_ROD)
        val damage = material.get()?.maxDurability?.let { maxDurability ->
            val percent = skillCooldownMap[owner.uniqueId]?.get(skill.key)?.percent(owner) ?: 1.0
            if (percent < 1) (maxDurability.cdouble * percent).cint else null
        }
        return SlotSnapshot(
            skillKey = skill.key,
            material = material,
            name = skill.getIcon(),
            lore = skill.getDescriptionComparison().toList(),
            amount = index + 1,
            damage = damage
        )
    }

    private fun applySnapshot(index: Int, snapshot: SlotSnapshot) {
        val current = viewer.inventory.getItem(index)
        if (snapshots[index] == snapshot) {
            val renderedItem = renderedItems[index]
            if (current != renderedItem) {
                viewer.inventory.setItem(index, renderedItem?.clone())
            }
            return
        }
        val item = buildItem(snapshot.material) {
            name = snapshot.name
            lore += snapshot.lore
            amount = snapshot.amount
            hideAll()
            unique()
            snapshot.damage?.let { damage = it }
            colored()
        }
        snapshots[index] = snapshot
        renderedItems[index] = item.clone()
        if (current != item) {
            viewer.inventory.setItem(index, item)
        }
    }

    override fun close() {
        remove()
        slotIndex.forEach { index ->
            getBindKey("MC" + (index + 1)) ?: return@forEach
            viewer.inventory.setItem(index, null)
            snapshots[index] = null
            renderedItems[index] = null
        }
    }

    protected open fun remove() {
        val indexedHud = bukkitSkillHudByViewer.remove(viewer.uniqueId)
        val huds = if (indexedHud == null || indexedHud === this) listOf(this) else listOf(indexedHud, this)
        huds.forEach { hud ->
            hud.active = false
            bukkitSkillHudMap[hud.owner.uniqueId]?.let { ownerHuds ->
                ownerHuds.remove(hud.viewer.uniqueId, hud)
                if (ownerHuds.isEmpty()) {
                    bukkitSkillHudMap.remove(hud.owner.uniqueId, ownerHuds)
                }
            }
        }
    }
}
