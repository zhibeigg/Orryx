package org.gitee.orryx.module.ui.germplugin

import com.germ.germplugin.api.dynamic.gui.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.module.ui.AbstractSkillUI
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.utils.*
import taboolib.common.function.debounce
import taboolib.common.platform.function.getDataFolder
import java.io.File

open class GermPluginSkillUI(override val viewer: Player, override val owner: Player): AbstractSkillUI(viewer, owner) {

    private val debouncedUpdate = debounce(50L) {
        updateNow()
    }

    companion object {

        internal lateinit var skillUIConfiguration: YamlConfiguration

        @Reload(weight = 2)
        private fun reload() {
            if (IUIManager.INSTANCE !is GermPluginUIManager) return
            skillUIConfiguration = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/germplugin/OrryxSkillUI.yml"))
        }
    }

    override fun open() {
        owner.job {
            open(it)
        }
    }

    override fun update() {
        debouncedUpdate()
    }

    private fun updateNow() {
        owner.job { job ->
            job.bindSkills {
                it.forEach { entry ->
                    val bindKeyIcon = screen
                        .pickPart<GermGuiScroll>("skillBindKeyScroll")
                        .pickPart<GermGuiCanvas>("skillBindKey-canvas-${entry.key.key}")
                        .pickPart<GermGuiTexture>("skillBindKey-icon-${entry.key.key}")
                    if (entry.value != null) {
                        bindKeyIcon.path = path.replace("{skill}", entry.value!!.key)
                        bindKeyIcon.enable = true
                    } else {
                        bindKeyIcon.enable = false
                    }
                }
            }
        }
    }

    private lateinit var path: String

    protected open var cursorSkill: IPlayerSkill? = null
    protected open lateinit var screen: GermGuiScreen

    protected open fun open(job: IPlayerJob) {
        screen = GermGuiScreen.getGermGuiScreen("OrryxSkillUI", skillUIConfiguration)
        owner.orryxProfile { profile ->
            job.skills { skills ->
                val skillScroll = screen.pickPart<GermGuiScroll>("skillScroll")
                val skillBackgroundBase = screen.pickPart<GermGuiButton>("skillBackground")
                val skillIconBase = screen.pickPart<GermGuiButton>("skillIcon")
                val skillListNameBase = screen.pickPart<GermGuiLabel>("skillListName")
                val skillLevelUpBase = screen.pickPart<GermGuiButton>("skillLevelUp")

                val name = screen.pickPart<GermGuiLabel>("skillName")
                val description = screen.pickPart<GermGuiLabel>("skillDescription")

                val cursor = screen.pickPart<GermGuiTexture>("cursor")

                val skillBindKeyScroll = screen.pickPart<GermGuiScroll>("skillBindKeyScroll")
                val bindKeyBackgroundBase = screen.pickPart<GermGuiButton>("skillBindKeyBackground")
                val bindKeyLabelBase = screen.pickPart<GermGuiLabel>("skillBindKey")
                val bindKeyIconBase = screen.pickPart<GermGuiTexture>("skillBindKeyIcon")
                path = cursor.path

                skills.forEach { skill ->
                    val background = GermGuiButton("skillIcon-background-${skill.key}").copyFrom(skillBackgroundBase)
                    val skillIcon = GermGuiButton("skillIcon-${skill.key}").copyFrom(skillIconBase)
                    val skillListName = GermGuiLabel("skillListName-${skill.key}").copyFrom(skillListNameBase)
                    val skillLevelUp = GermGuiButton("skillLevelUp-${skill.key}").copyFrom(skillLevelUpBase)

                    background.callback(GermGuiButton.EventType.LEFT_CLICK) { _, _ ->
                        name.setText(skill.skill.name)
                        description.texts = skill.getDescriptionComparison()
                    }
                    background.enable = true

                    skillIcon.defaultPath = skillIcon.defaultPath.replace("{skill}", skill.key)
                    skillIcon.hoverPath = skillIcon.hoverPath.replace("{skill}", skill.key)
                    skillIcon.callback(GermGuiButton.EventType.LEFT_CLICK, GermGuiButton.EventType.RIGHT_CLICK) { _, _ ->
                        cursorSkill = skill
                        cursor.setPath(path.replace("{skill}", skill.key))
                        cursor.enable = true
                    }
                    skillIcon.enable = true

                    skillListName.setText(skill.skill.name)
                    skillListName.enable = true

                    skill.upgradePointCheck(skill.level, skill.level + 1).thenAccept { pointCheck ->
                        skillLevelUp.tooltip = listOf(
                            "&e等级 &f${skill.level}&7/&f${skill.skill.maxLevel}",
                            "${if (pointCheck.second) "&a" else "&c"}右键!升级 &f消耗技能点 &e${pointCheck.first}",
                            "&f总技能点 &e${profile.point}"
                        )
                        skillLevelUp.callback(GermGuiButton.EventType.RIGHT_CLICK) { _, _ ->
                            skill.up()
                            skillLevelUp.tooltip = listOf(
                                "&e等级 &f${skill.level}&7/&f${skill.skill.maxLevel}",
                                "${if (pointCheck.second) "&a" else "&c"}右键!升级 &f消耗技能点 &e${pointCheck.first}",
                                "&f总技能点 &e${profile.point}"
                            )
                        }
                        skillLevelUp.enable = true
                    }

                    val canvas = GermGuiCanvas("skillIcon-canvas-${skill.key}")
                    canvas.width =
                        "%${screen.guiName}_${skillScroll.indexName}$${canvas.indexName}$${background.indexName}_width%"
                    canvas.height =
                        "%${screen.guiName}_${skillScroll.indexName}$${canvas.indexName}$${background.indexName}_height%"
                    canvas.enable = true

                    canvas.addGuiPart(background)
                    canvas.addGuiPart(skillIcon)
                    canvas.addGuiPart(skillListName)
                    canvas.addGuiPart(skillLevelUp)
                    skillScroll.addGuiPart(canvas)
                }
                skillScroll.enable = true

                job.bindSkills { bind ->
                    bindKeys().forEach {
                        val bindKeyBackground =
                            GermGuiButton("skillBindKey-background-${it.key}").copyFrom(bindKeyBackgroundBase)
                        val bindKeyLabel = GermGuiLabel("skillBindKey-label-${it.key}").copyFrom(bindKeyLabelBase)
                        val bindKeyIcon = GermGuiTexture("skillBindKey-icon-${it.key}").copyFrom(bindKeyIconBase)

                        bindKeyBackground.callback(GermGuiButton.EventType.LEFT_CLICK) { _, _ ->
                            if (cursorSkill != null) {
                                bindSkill(job, cursorSkill!!.key, job.group, it.key).thenAccept { success ->
                                    if (success) {
                                        update()
                                        cursorSkill = null
                                        cursor.enable = false
                                    }
                                }
                            }
                        }
                        bindKeyBackground.callback(GermGuiButton.EventType.RIGHT_CLICK) { _, _ ->
                            if (cursorSkill != null) {
                                bindSkill(job, cursorSkill!!.key, job.group, it.key).thenAccept { success ->
                                    if (success) {
                                        update()
                                        cursorSkill = null
                                        cursor.enable = false
                                    }
                                }
                            } else {
                                job.bindSkills { bindSkills ->
                                    val skill = bindSkills[it] ?: return@bindSkills
                                    unBindSkill(job, skill.key, job.group).thenAccept { success ->
                                        if (success) {
                                            bindKeyIcon.enable = false
                                        }
                                    }
                                }
                            }
                        }
                        bindKeyBackground.enable = true

                        bindKeyLabel.setText(it.key)
                        bindKeyLabel.enable = true

                        val skill = bind[it]
                        if (bind[it] != null) {
                            bindKeyIcon.path = bindKeyIcon.path.replace("{skill}", skill!!.key)
                            bindKeyIcon.enable = true
                        }

                        val canvas = GermGuiCanvas("skillBindKey-canvas-${it.key}")
                        canvas.width =
                            "%${screen.guiName}_${skillBindKeyScroll.indexName}$${canvas.indexName}$${bindKeyBackground.indexName}_width%"
                        canvas.height =
                            "%${screen.guiName}_${skillBindKeyScroll.indexName}$${canvas.indexName}$${bindKeyBackground.indexName}_height%"
                        canvas.addGuiPart(bindKeyBackground)
                        canvas.addGuiPart(bindKeyLabel)
                        canvas.addGuiPart(bindKeyIcon)
                        canvas.enable = true

                        skillBindKeyScroll.addGuiPart(canvas)
                    }
                    skillBindKeyScroll.enable = true
                    screen.removeGuiPart(skillBackgroundBase)
                    screen.removeGuiPart(skillIconBase)
                    screen.removeGuiPart(skillListNameBase)
                    screen.removeGuiPart(skillLevelUpBase)

                    screen.removeGuiPart(bindKeyBackgroundBase)
                    screen.removeGuiPart(bindKeyLabelBase)
                    screen.removeGuiPart(bindKeyIconBase)
                    screen.openGui(viewer)
                }
            }
        }
    }
}
