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
    }

    private lateinit var job: IPlayerJob
    protected open var cursorSkill: IPlayerSkill? = null

    protected open lateinit var screen: GermGuiScreen

    protected open fun build(job: IPlayerJob): GermGuiScreen {
        screen = GermGuiScreen.getGermGuiScreen("OrryxSkillUI", skillUIConfiguration)
        val skills = owner.getSkills()

        val skillScroll = screen.getGuiPart("skillScroll") as GermGuiScroll
        val skillBackgroundBase = skillScroll.getGuiPart("skillBackground") as GermGuiButton
        val skillIconBase = skillScroll.getGuiPart("skillIcon") as GermGuiButton

        val name = screen.getGuiPart("skillName") as GermGuiLabel
        val description = screen.getGuiPart("skillDescription") as GermGuiLabel

        val cursor = screen.getGuiPart("cursor") as GermGuiTexture

        val skillBindKeyScroll = screen.getGuiPart("skillBindKeyScroll") as GermGuiScroll
        val bindKeyBackgroundBase = screen.getGuiPart("skillBindKeyBackground") as GermGuiButton
        val bindKeyLabelBase = screen.getGuiPart("skillBindKey") as GermGuiLabel
        val bindKeyIconBase = screen.getGuiPart("skillBindKeyIcon") as GermGuiTexture
        val path = cursor.path

        skills.forEach { skill ->
            val background = GermGuiButton("skillIcon_background_${skill.key}").copyFrom(skillBackgroundBase)
            val skillIcon = GermGuiButton("skillIcon_${skill.key}").copyFrom(skillIconBase)

            skillIcon.defaultPath = skillIcon.defaultPath.replace("{skill}", skill.key)
            skillIcon.hoverPath = skillIcon.hoverPath.replace("{skill}", skill.key)
            background.registerCallbackHandler(
                { _, _ ->
                    name.setText(skill.skill.name)
                    description.setTexts(skill.getDescriptionComparison())
                },
                GermGuiButton.EventType.LEFT_CLICK
            )
            skillIcon.registerCallbackHandler(
                { _, _ ->
                    cursorSkill = skill
                    cursor.setPath(path.replace("{skill}", skill.key))
                    cursor.enable = true
                },
                GermGuiButton.EventType.LEFT_CLICK,
                GermGuiButton.EventType.RIGHT_CLICK
            )
            val canvas = GermGuiCanvas("skillIcon_canvas_${skill.key}")
            canvas.width = skillBackgroundBase.width
            canvas.height = skillBackgroundBase.height
            canvas.addGuiPart(background)
            canvas.addGuiPart(skillIcon)
            skillScroll.addGuiPart(canvas)
        }
        val bind = job.getBindSkills()
        bindKeys().forEach {
            val bindBackground = GermGuiButton("skillBindKey_background_${it.key}").copyFrom(bindKeyBackgroundBase)
            val bindKeyLabel = GermGuiLabel("skillBindKey_label_${it.key}").copyFrom(bindKeyLabelBase)
            val bindKeyIcon = GermGuiTexture("skillBindKey_icon_${it.key}").copyFrom(bindKeyIconBase)

            bindBackground.registerCallbackHandler(
                { _, _ ->
                    if (cursorSkill != null) {
                        if (bindSkill(job, cursorSkill!!.key, job.group, it.key)) {
                            cursorSkill = null
                            cursor.enable = false
                            bindKeyIcon.path = bindKeyIconBase.path.replace("{skill}", cursorSkill!!.key)
                            bindKeyIcon.enable = true
                        }
                    }
                },
                GermGuiButton.EventType.LEFT_CLICK
            )
            bindBackground.registerCallbackHandler(
                { _, _ ->
                    if (cursorSkill != null) {
                        if (bindSkill(job, cursorSkill!!.key, job.group, it.key)) {
                            cursorSkill = null
                            cursor.enable = false
                            bindKeyIcon.path = bindKeyIconBase.path.replace("{skill}", cursorSkill!!.key)
                            bindKeyIcon.enable = true
                        }
                    } else {
                        if (unBindSkill(job, job.group, it.key)) {
                            bindKeyIcon.enable = false
                        }
                    }
                },
                GermGuiButton.EventType.RIGHT_CLICK
            )
            bindKeyLabel.setText(it.key)

            val canvas = GermGuiCanvas("skillBindKey_canvas_${it.key}")

            val skill = bind[it]
            if (bind[it] != null) {
                bindKeyIcon.path = bindKeyIcon.path.replace("{skill}", skill!!.key)
                bindKeyIcon.enable = true
            }

            canvas.addGuiPart(bindBackground)
            canvas.addGuiPart(bindKeyLabel)
            canvas.addGuiPart(bindKeyIcon)
            skillBindKeyScroll.addGuiPart(canvas)
        }
        screen.removeGuiPart(skillBackgroundBase)
        screen.removeGuiPart(skillIconBase)

        screen.removeGuiPart(bindKeyBackgroundBase)
        screen.removeGuiPart(bindKeyLabelBase)
        screen.removeGuiPart(bindKeyIconBase)
        return screen
    }

}