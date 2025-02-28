package org.gitee.orryx.core.ui.germplugin

import com.germ.germplugin.api.dynamic.gui.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.core.ui.AbstractSkillUI
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.getDataFolder
import java.io.File

open class GermPluginSkillUI(override val viewer: Player, override val owner: Player): AbstractSkillUI(viewer, owner) {

    companion object {

        private val skillUIConfiguration
            get() = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/germplugin/OrryxSkillUI.yml"))

    }

    override fun open() {
        job = owner.job() ?: return
        build(job).openGui(viewer)
    }

    override fun update() {
        job.getBindSkills().forEach {
            val bindKeyIcon = screen
                    .pickPart<GermGuiScroll>("skillBindKeyScroll")
                    .pickPart<GermGuiCanvas>("skillBindKey-canvas-${it.key.key}")
                    .pickPart<GermGuiTexture>("skillBindKey-icon-${it.key.key}")
            if (it.value != null) {
                bindKeyIcon.path = path.replace("{skill}", it.value!!.key)
                bindKeyIcon.enable = true
            } else {
                bindKeyIcon.enable = false
            }
        }
    }

    private lateinit var job: IPlayerJob
    private lateinit var path: String

    protected open var cursorSkill: IPlayerSkill? = null
    protected open lateinit var screen: GermGuiScreen

    protected open fun build(job: IPlayerJob): GermGuiScreen {
        screen = GermGuiScreen.getGermGuiScreen("OrryxSkillUI", skillUIConfiguration)
        val skills = owner.getSkills()

        val skillScroll = screen.pickPart<GermGuiScroll>("skillScroll")
        val skillBackgroundBase = screen.pickPart<GermGuiButton>("skillBackground")
        val skillIconBase = screen.pickPart<GermGuiButton>("skillIcon")
        val skillListNameBase = screen.pickPart<GermGuiLabel>("skillListName")

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

            background.callback(GermGuiButton.EventType.LEFT_CLICK) { _, _ ->
                name.setText(skill.skill.name)
                description.setTexts(skill.getDescriptionComparison())
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

            val canvas = GermGuiCanvas("skillIcon-canvas-${skill.key}")
            canvas.width = "%${screen.guiName}_${skillScroll.indexName}$${canvas.indexName}$${background.indexName}_width%"
            canvas.height = "%${screen.guiName}_${skillScroll.indexName}$${canvas.indexName}$${background.indexName}_height%"
            canvas.enable = true

            canvas.addGuiPart(background)
            canvas.addGuiPart(skillIcon)
            canvas.addGuiPart(skillListName)
            skillScroll.addGuiPart(canvas)
        }
        skillScroll.enable = true

        val bind = job.getBindSkills()
        bindKeys().forEach {
            val bindKeyBackground = GermGuiButton("skillBindKey-background-${it.key}").copyFrom(bindKeyBackgroundBase)
            val bindKeyLabel = GermGuiLabel("skillBindKey-label-${it.key}").copyFrom(bindKeyLabelBase)
            val bindKeyIcon = GermGuiTexture("skillBindKey-icon-${it.key}").copyFrom(bindKeyIconBase)

            bindKeyBackground.callback(GermGuiButton.EventType.LEFT_CLICK) { _, _ ->
                if (cursorSkill != null) {
                    if (bindSkill(job, cursorSkill!!.key, job.group, it.key)) {
                        update()
                        cursorSkill = null
                        cursor.enable = false
                    }
                }
            }
            bindKeyBackground.callback(GermGuiButton.EventType.RIGHT_CLICK) { _, _ ->
                if (cursorSkill != null) {
                    if (bindSkill(job, cursorSkill!!.key, job.group, it.key)) {
                        update()
                        cursorSkill = null
                        cursor.enable = false
                    }
                } else {
                    val skill = job.getBindSkills()[it] ?: return@callback
                    if (unBindSkill(job, skill.key, job.group)) {
                        bindKeyIcon.enable = false
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
            canvas.width = "%${screen.guiName}_${skillBindKeyScroll.indexName}$${canvas.indexName}$${bindKeyBackground.indexName}_width%"
            canvas.height = "%${screen.guiName}_${skillBindKeyScroll.indexName}$${canvas.indexName}$${bindKeyBackground.indexName}_height%"
            canvas.addGuiPart(bindKeyBackground)
            canvas.addGuiPart(bindKeyLabel)
            canvas.addGuiPart(bindKeyIcon)
            canvas.enable = true

            skillBindKeyScroll.addGuiPart(canvas)
        }
        skillBindKeyScroll.enable = true
        screen.removeGuiPart(skillBackgroundBase)
        screen.removeGuiPart(skillIconBase)

        screen.removeGuiPart(bindKeyBackgroundBase)
        screen.removeGuiPart(bindKeyLabelBase)
        screen.removeGuiPart(bindKeyIconBase)
        return screen
    }

}