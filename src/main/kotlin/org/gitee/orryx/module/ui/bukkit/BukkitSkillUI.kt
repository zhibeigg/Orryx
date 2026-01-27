package org.gitee.orryx.module.ui.bukkit

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.module.ui.AbstractSkillUI
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.utils.*
import taboolib.common.function.debounce
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.impl.PageableChestImpl
import taboolib.platform.util.buildItem
import kotlin.jvm.optionals.getOrNull
import kotlin.math.ceil

open class BukkitSkillUI(override val viewer: Player, override val owner: Player): AbstractSkillUI(viewer, owner) {

    private val debouncedUpdate = debounce(50L) {
        updateNow()
    }

    companion object {

        internal lateinit var ui: UI

        class UI(configurationSection: ConfigurationSection) {

            val title = configurationSection.getString("title", "技能界面")!!

            val skills = Item(configurationSection, "Skills")

            val bindSkills = Item(configurationSection, "BindSkills")

            val space = Item(configurationSection, "Space")

            val previous = Item(configurationSection, "Previous")

            val next = Item(configurationSection, "Next")

            class Item(configurationSection: ConfigurationSection, key: String) {

                val name = configurationSection.getString("$key.Name", "")

                val lore = configurationSection.getStringList("$key.Lore")

                val amount = configurationSection.getInt("$key.Amount", 1)

                val xMaterial: XMaterial? = configurationSection.getString("$key.XMaterial")?.let { XMaterial.matchXMaterial(it).getOrNull() }

                val slots = configurationSection.getIntegerList("$key.Slots")

            }

        }

        @Reload(weight = 2)
        private fun reload() {
            if (IUIManager.INSTANCE !is BukkitUIManager) return
            ui = UI(IUIManager.INSTANCE.config.getConfigurationSection("SkillUI")!!)
        }
    }

    protected open var cursorSkill: IPlayerSkill? = null

    protected open val isWrite
        get() = viewer == owner || viewer.isOp

    protected open lateinit var inventory: Inventory
    private lateinit var job: IPlayerJob
    private lateinit var skills: List<IPlayerSkill>
    private lateinit var bindSkills: MutableMap<IBindKey, IPlayerSkill?>

    override fun open() {
        owner.job {
            job = it
            it.skills { skills ->
                this.skills = skills
                it.bindSkills { bindSkills ->
                    this.bindSkills = bindSkills.toMutableMap()
                    viewer.openMenu(build().also { inventory = it })
                }
            }
        }
    }

    protected open fun build(): Inventory {
        return buildMenu<PageableChestImpl<IPlayerSkill>>(ui.title) {
            rows(6)
            handLocked(false)
            menuLocked(true)

            slots(ui.skills.slots)
            elements { skills }

            ui.space.slots.forEach {
                set(it) {
                    buildItem(ui.space.xMaterial ?: XMaterial.GRAY_STAINED_GLASS_PANE) {
                        name = ui.space.name
                        lore += ui.space.lore
                        amount = ui.space.amount
                        hideAll()
                        colored()
                    }
                }
            }

            ui.previous.slots.forEach {
                setPreviousPage(it) { _, _ ->
                    buildItem(ui.previous.xMaterial ?: XMaterial.PAPER) {
                        name = ui.previous.name
                        lore += ui.previous.lore
                        amount = page + 1
                        hideAll()
                        colored()
                    }
                }
            }

            ui.next.slots.forEach {
                setNextPage(it) { _, _ ->
                    buildItem(ui.next.xMaterial ?: XMaterial.PAPER) {
                        name = ui.next.name
                        lore += ui.next.lore
                        amount = ceil(elementsCache.size.cdouble / menuSlots.size.cdouble).cint
                        hideAll()
                        colored()
                    }
                }
            }

            bindKeys().forEachIndexed { index, iBindKey ->
                bindSkills[iBindKey]?.apply {
                    set(ui.bindSkills.slots[index]) {
                        buildItem(XMaterial.matchXMaterial(skill.xMaterial).orElse(XMaterial.BLAZE_ROD)) {
                            name = getIcon()
                            lore += getDescriptionComparison()
                            lore += ""
                            lore += "&a| &c左键&f将技能绑定在此格子"
                            lore += "&a| &c右键&f将此格技能解绑"
                            amount = index + 1
                            hideAll()
                            colored()
                        }
                    }
                } ?: run {
                    set(ui.bindSkills.slots[index]) {
                        buildItem(XMaterial.BARRIER) {
                            name = "空技能槽"
                            lore += "&a| &c左键&f将技能绑定在此格子"
                            amount = index + 1
                            hideAll()
                            colored()
                        }
                    }
                }
            }

            onGenerate { _, element, index, _ ->
                buildItem(XMaterial.matchXMaterial(element.skill.xMaterial).orElse(XMaterial.BLAZE_ROD)) {
                    name = element.getIcon()
                    lore += element.getDescriptionComparison()
                    lore += ""
                    lore += "&a| &c左键&f选中此技能"
                    amount = index + 1
                    hideAll()
                    colored()
                }
            }
            onClick { event, element ->
                if (isWrite) {
                    viewer.setItemOnCursor(event.inventory.getItem(event.rawSlot))
                    cursorSkill = element
                }
            }
            onClick(true) { event ->
                if (event.rawSlot == -999) {
                    viewer.setItemOnCursor(null)
                    cursorSkill = null
                }
                if (event.rawSlot in ui.bindSkills.slots && isWrite) {
                    val index = ui.bindSkills.slots.indexOf(event.rawSlot)
                    val bindKeys = bindKeys()
                    if (bindKeys.lastIndex < index) return@onClick
                    when {
                        event.clickEvent().isLeftClick && cursorSkill != null -> {
                            bindSkill(
                                job,
                                cursorSkill!!.skill.key,
                                job.group,
                                bindKeys[index].key
                            ).thenAccept { success ->
                                if (success) {
                                    update()
                                    viewer.setItemOnCursor(null)
                                    cursorSkill = null
                                }
                            }
                        }

                        event.clickEvent().isRightClick -> {
                            owner.getGroupSkills(job.group).thenApply {
                                it?.get(bindKeys[index])?.let { skill ->
                                    unBindSkill(job, skill, job.group).thenAccept { success ->
                                        if (success) {
                                            update()
                                            viewer.setItemOnCursor(null)
                                            cursorSkill = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (event.clickEvent().isRightClick) {
                        viewer.setItemOnCursor(null)
                        cursorSkill = null
                    }
                }
            }
            onClose {
                viewer.setItemOnCursor(null)
                cursorSkill = null
            }
        }
    }

    override fun update() {
        debouncedUpdate()
    }

    private fun updateNow() {
        owner.job {
            it.getBindSkills()
        }
        bindKeys().forEachIndexed { index, iBindKey ->
            bindSkills[iBindKey]?.apply {
                inventory.setItem(
                    ui.bindSkills.slots[index],
                    buildItem(XMaterial.matchXMaterial(skill.xMaterial).orElse(XMaterial.BLAZE_ROD)) {
                        name = getIcon()
                        lore += getDescriptionComparison()
                        lore += ""
                        lore += "&a| &c左键&f将技能绑定在此格子"
                        lore += "&a| &c右键&f将此格技能解绑"
                        amount = index + 1
                        hideAll()
                        colored()
                    }
                )
            } ?: run {
                inventory.setItem(
                    ui.bindSkills.slots[index],
                    buildItem(XMaterial.BARRIER) {
                        name = "空技能槽"
                        lore += "&a| &c左键&f将技能绑定在此格子"
                        amount = index + 1
                        hideAll()
                        colored()
                    }
                )
            }
        }
    }
}
