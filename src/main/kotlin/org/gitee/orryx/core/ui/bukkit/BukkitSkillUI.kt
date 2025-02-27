package org.gitee.orryx.core.ui.bukkit

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.core.ui.AbstractSkillUI
import org.gitee.orryx.core.ui.IUIManager
import org.gitee.orryx.utils.*
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.buildMenu
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.impl.PageableChestImpl
import taboolib.platform.util.buildItem
import kotlin.jvm.optionals.getOrNull
import kotlin.math.ceil

open class BukkitSkillUI(override val viewer: Player, override val owner: Player): AbstractSkillUI(viewer, owner) {

    companion object {

        private val configurationSection
            get() = IUIManager.INSTANCE.config.getConfigurationSection("SkillUI")!!

        val title
            get() = configurationSection.getString("title", "技能界面")!!

        val skills
            get() = Item("Skills")

        val bindSkills
            get() = Item("BindSkills")

        val space
            get() = Item("Space")

        val previous
            get() = Item("Previous")

        val next
            get() = Item("Next")

        class Item(private val key: String) {

            val name
                get() = configurationSection.getString("$key.Name", "")

            val lore
                get() = configurationSection.getStringList("$key.Lore")

            val amount
                get() = configurationSection.getInt("$key.Amount", 1)

            val xMaterial: XMaterial?
                get() = configurationSection.getString("$key.XMaterial")?.let { XMaterial.matchXMaterial(it).getOrNull() }

            val slots
                get() = configurationSection.getIntegerList("$key.Slots")

        }

    }

    protected open var cursorSkill: IPlayerSkill? = null

    protected open val isWrite
        get() = viewer == owner || viewer.isOp

    protected open lateinit var inventory: Inventory
    private lateinit var job: IPlayerJob

    override fun open() {
        job = owner.job() ?: return
        viewer.openMenu(build(job).also { inventory = it })
    }

    protected open fun build(job: IPlayerJob): Inventory {
        return buildMenu<PageableChestImpl<IPlayerSkill>>(title) {
            rows(6)
            handLocked(false)
            menuLocked(true)

            slots(skills.slots)
            elements { owner.getSkills() }

            space.slots.forEach {
                set(it) {
                    buildItem(space.xMaterial ?: XMaterial.GRAY_STAINED_GLASS_PANE) {
                        name = space.name
                        lore += space.lore
                        amount = space.amount
                        hideAll()
                        colored()
                    }
                }
            }

            previous.slots.forEach {
                setPreviousPage(it) { _, _ ->
                    buildItem(previous.xMaterial ?: XMaterial.PAPER) {
                        name = previous.name
                        lore += previous.lore
                        amount = page + 1
                        hideAll()
                        colored()
                    }
                }
            }

            next.slots.forEach {
                setNextPage(it) { _, _ ->
                    buildItem(next.xMaterial ?: XMaterial.PAPER) {
                        name = next.name
                        lore += next.lore
                        amount = ceil(elementsCache.size.cdouble / menuSlots.size.cdouble).cint
                        hideAll()
                        colored()
                    }
                }
            }

            val bindSkillMap = job.getBindSkills()
            bindKeys().forEachIndexed { index, iBindKey ->
                bindSkillMap[iBindKey]?.apply {
                    set(bindSkills.slots[index]) {
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
                    set(bindSkills.slots[index]) {
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
                if (event.rawSlot in bindSkills.slots && isWrite) {
                    val index = bindSkills.slots.indexOf(event.rawSlot)
                    val bindKeys = bindKeys()
                    if (bindKeys.lastIndex < index) return@onClick
                    when {
                        event.clickEvent().isLeftClick && cursorSkill != null -> {
                            if (bindSkill(job, cursorSkill!!.skill.key, job.group, bindKeys[index].key)) {
                                update()
                                viewer.setItemOnCursor(null)
                                cursorSkill = null
                            }
                        }
                        event.clickEvent().isRightClick -> {
                            owner.getGroupSkills(job.group)[bindKeys[index]]?.let {
                                if (unBindSkill(job, it, job.group)) {
                                    update()
                                    viewer.setItemOnCursor(null)
                                    cursorSkill = null
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
        val bindSkillMap = job.getBindSkills()
        bindKeys().forEachIndexed { index, iBindKey ->
            bindSkillMap[iBindKey]?.apply {
                inventory.setItem(
                    bindSkills.slots[index],
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
                    bindSkills.slots[index],
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