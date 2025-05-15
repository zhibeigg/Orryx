package org.gitee.orryx.module.ui.germplugin

import com.germ.germplugin.api.dynamic.gui.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.module.ui.AbstractSkillHud
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.module.ui.IUIManager.Companion.skillCooldownMap
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.submitAsync
import taboolib.common.util.unsafeLazy
import java.io.File
import java.util.*
import kotlin.collections.set

open class GermPluginSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    companion object {

        /**
         * owner, viewer, [GermPluginSkillHud]
         */
        internal val germSkillHudMap by unsafeLazy { hashMapOf<UUID, MutableMap<UUID, GermPluginSkillHud>>() }

        fun getViewerHud(player: Player): GermPluginSkillHud? {
            return germSkillHudMap.firstNotNullOfOrNull {
                it.value[player.uniqueId]
            }
        }

        @Reload(2)
        private fun reload() {
            if (IUIManager.INSTANCE !is GermPluginUIManager) return
            skillHUDConfiguration = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/germplugin/OrryxSkillHUD.yml"))
            germSkillHudMap.forEach {
                it.value.forEach { map ->
                    map.value.update()
                }
            }
        }

        internal lateinit var skillHUDConfiguration: YamlConfiguration
    }

    protected open lateinit var screen: GermGuiScreen
    protected open var isH = true

    override fun update(skill: IPlayerSkill?) {
        if (isH) {
            screen.pickPart<GermGuiCanvas>("skillBindCanvasV").enable = false
            screen.pickPart<GermGuiCanvas>("canvasV").enable = false
            updateH()
        } else {
            screen.pickPart<GermGuiCanvas>("skillBindCanvasH").enable = false
            screen.pickPart<GermGuiCanvas>("canvasH").enable = false
            updateV()
        }
    }

    private fun updateH() {
        val skillBindCanvas = screen.pickPart<GermGuiCanvas>("skillBindCanvasH")
        val bindKeyBackgroundBase = screen.pickPart<GermGuiTexture>("skillBindKeyBackgroundH")
        val bindKeyLabelBase = screen.pickPart<GermGuiLabel>("skillBindKeyH")
        val bindKeyIconBase = screen.pickPart<GermGuiTexture>("skillBindKeyIconH")
        val bindKeyCooldownBase = screen.pickPart<GermGuiColor>("skillBindKeyCooldownH")
        val bindKeyCooldownLabelBase = screen.pickPart<GermGuiLabel>("skillBindKeyCooldownLabelH")
        val checkButton = screen.pickPart<GermGuiButton>("checkButton")

        checkButton.callback(GermGuiButton.EventType.RIGHT_CLICK) { _, _ ->
            isH = false
            update()
        }

        owner.job { job ->
            job.bindSkills { bind ->
                val bindKeys = bindKeys()
                val width = StringBuilder()
                bindKeys.forEach {
                    val bindKeyBackground = GermGuiTexture("skillBindKeyH-background-${it.key}").copyFrom(bindKeyBackgroundBase)
                    val bindKeyLabel = GermGuiLabel("skillBindKeyH-label-${it.key}").copyFrom(bindKeyLabelBase)
                    val bindKeyIcon = GermGuiTexture("skillBindKeyH-icon-${it.key}").copyFrom(bindKeyIconBase)
                    val bindKeyCooldown = GermGuiColor("skillBindKeyH-cooldown-${it.key}").copyFrom(bindKeyCooldownBase)
                    val bindKeyCooldownLabel = GermGuiLabel("skillBindKeyH-cooldown-label-${it.key}").copyFrom(bindKeyCooldownLabelBase)

                    bindKeyBackground.enable = true
                    bindKeyLabel.setText(it.key)
                    bindKeyLabel.enable = true

                    val canvas = GermGuiCanvas("skillBindKeyH-canvas-${it.key}")

                    val skill = bind[it]
                    if (bind[it] != null) {
                        bindKeyIcon.path = bindKeyIcon.path.replace("{skill}", skill!!.key)
                        bindKeyIcon.enable = true

                        val cooldown = skillCooldownMap[owner.uniqueId]?.get(skill.key)
                        bindKeyCooldown.tickDos = listOf(
                            "update<->${skillBindCanvas.indexName}\$${canvas.indexName}\$${bindKeyCooldown.indexName}@height@${bindKeyIcon.height}*(max(${cooldown?.getOverStamp(owner) ?: 0}-%time_now%,0)/${cooldown?.max ?: 1})",
                            "update<->${skillBindCanvas.indexName}\$${canvas.indexName}\$${bindKeyCooldown.indexName}@locationY@${bindKeyCooldown.locationY}+${bindKeyIcon.height}*(1-(max(${cooldown?.getOverStamp(owner) ?: 0}-%time_now%,0)/${cooldown?.max ?: 1}))"
                        )
                        bindKeyCooldown.enable = true

                        bindKeyCooldownLabel.setText("&c%countdown2_${cooldown?.getOverStamp(owner) ?: 0}%")
                        bindKeyCooldownLabel.enable = "notStr(%countdown2_${cooldown?.getOverStamp(owner) ?: 0}%,0.0s)"
                    }

                    canvas.width = "%${screen.guiName}_${skillBindCanvas.indexName}$${canvas.indexName}$${bindKeyBackground.indexName}_width%"
                    canvas.height = "%${screen.guiName}_${skillBindCanvas.indexName}$${canvas.indexName}$${bindKeyBackground.indexName}_height%"
                    canvas.addGuiPart(bindKeyBackground)
                    canvas.addGuiPart(bindKeyLabel)
                    canvas.addGuiPart(bindKeyIcon)
                    canvas.addGuiPart(bindKeyCooldown)
                    canvas.addGuiPart(bindKeyCooldownLabel)
                    canvas.enable = true

                    width.append(canvas.width)
                    width.append('+')

                    skillBindCanvas.addGuiPart(canvas)
                }
                val canvas = screen.pickPart<GermGuiCanvas>("canvasH")
                width.append("${skillBindCanvas.srcLayout.getString("gapX", "0")!!}*${bindKeys.size - 1}")
                skillBindCanvas.width = canvas.width
                skillBindCanvas.height = canvas.height
                skillBindCanvas.enable = true

                screen.options.drag.width = canvas.width
                screen.options.drag.height = canvas.height

                checkButton.locationX = canvas.locationX
                checkButton.locationY = canvas.locationY
                checkButton.width = canvas.width
                checkButton.height = canvas.height

                canvas.pickPart<GermGuiTexture>("background-center").width = width.toString()
                canvas.enable = true
                submitAsync {
                    screen.sendDos(listOf(
                        "updateOption<->OrryxSkillHUD@dragWidth@%OrryxSkillHUD_canvasH\$background-left_width%+%OrryxSkillHUD_canvasH\$background-center_width%+%OrryxSkillHUD_canvasH\$background-right_width%",
                        "updateOption<->OrryxSkillHUD@dragHeight@%OrryxSkillHUD_canvasH\$background-left_height%"
                    ))
                }
            }
        }
    }

    private fun updateV() {
        val skillBindCanvas = screen.pickPart<GermGuiCanvas>("skillBindCanvasV")
        val bindKeyBackgroundBase = screen.pickPart<GermGuiTexture>("skillBindKeyBackgroundV")
        val bindKeyLabelBase = screen.pickPart<GermGuiLabel>("skillBindKeyV")
        val bindKeyIconBase = screen.pickPart<GermGuiTexture>("skillBindKeyIconV")
        val bindKeyCooldownBase = screen.pickPart<GermGuiColor>("skillBindKeyCooldownV")
        val bindKeyCooldownLabelBase = screen.pickPart<GermGuiLabel>("skillBindKeyCooldownLabelV")
        val checkButton = screen.pickPart<GermGuiButton>("checkButton")

        checkButton.callback(GermGuiButton.EventType.RIGHT_CLICK) { _, _ ->
            isH = true
            update()
        }

        owner.job { job ->
            job.bindSkills { bind ->
                val bindKeys = bindKeys()
                val height = StringBuilder()
                bindKeys.forEach {
                    val bindKeyBackground =
                        GermGuiTexture("skillBindKeyV-background-${it.key}").copyFrom(bindKeyBackgroundBase)
                    val bindKeyLabel = GermGuiLabel("skillBindKeyV-label-${it.key}").copyFrom(bindKeyLabelBase)
                    val bindKeyIcon = GermGuiTexture("skillBindKeyV-icon-${it.key}").copyFrom(bindKeyIconBase)
                    val bindKeyCooldown = GermGuiColor("skillBindKeyV-cooldown-${it.key}").copyFrom(bindKeyCooldownBase)
                    val bindKeyCooldownLabel =
                        GermGuiLabel("skillBindKeyV-cooldown-label-${it.key}").copyFrom(bindKeyCooldownLabelBase)

                    bindKeyBackground.enable = true
                    bindKeyLabel.setText(it.key)
                    bindKeyLabel.enable = true

                    val canvas = GermGuiCanvas("skillBindKeyV-canvas-${it.key}")

                    val skill = bind[it]
                    if (bind[it] != null) {
                        bindKeyIcon.path = bindKeyIcon.path.replace("{skill}", skill!!.key)
                        bindKeyIcon.enable = true

                        val cooldown = skillCooldownMap[owner.uniqueId]?.get(skill.key)
                        bindKeyCooldown.tickDos = listOf(
                            "update<->${skillBindCanvas.indexName}\$${canvas.indexName}\$${bindKeyCooldown.indexName}@height@${bindKeyIcon.height}*(max(${
                                cooldown?.getOverStamp(
                                    owner
                                ) ?: 0
                            }-%time_now%,0)/${cooldown?.max ?: 1})",
                            "update<->${skillBindCanvas.indexName}\$${canvas.indexName}\$${bindKeyCooldown.indexName}@locationY@${bindKeyCooldown.locationY}+${bindKeyIcon.height}*(1-(max(${
                                cooldown?.getOverStamp(
                                    owner
                                ) ?: 0
                            }-%time_now%,0)/${cooldown?.max ?: 1}))"
                        )
                        bindKeyCooldown.enable = true

                        bindKeyCooldownLabel.setText("&c%countdown2_${cooldown?.getOverStamp(owner) ?: 0}%")
                        bindKeyCooldownLabel.enable = "notStr(%countdown2_${cooldown?.getOverStamp(owner) ?: 0}%,0.0s)"
                    }

                    canvas.width =
                        "%${screen.guiName}_${skillBindCanvas.indexName}$${canvas.indexName}$${bindKeyBackground.indexName}_width%"
                    canvas.height =
                        "%${screen.guiName}_${skillBindCanvas.indexName}$${canvas.indexName}$${bindKeyBackground.indexName}_height%"
                    canvas.addGuiPart(bindKeyBackground)
                    canvas.addGuiPart(bindKeyLabel)
                    canvas.addGuiPart(bindKeyIcon)
                    canvas.addGuiPart(bindKeyCooldown)
                    canvas.addGuiPart(bindKeyCooldownLabel)
                    canvas.enable = true

                    height.append(canvas.height)
                    height.append('+')

                    skillBindCanvas.addGuiPart(canvas)
                }
                val canvas = screen.pickPart<GermGuiCanvas>("canvasV")
                height.append("${skillBindCanvas.srcLayout.getString("gapY", "0")!!}*${bindKeys.size - 1}")
                skillBindCanvas.width = canvas.width
                skillBindCanvas.height = canvas.height
                skillBindCanvas.enable = true

                checkButton.locationX = canvas.locationX
                checkButton.locationY = canvas.locationY
                checkButton.width = canvas.width
                checkButton.height = canvas.height

                canvas.pickPart<GermGuiTexture>("background-center").height = height.toString()
                canvas.enable = true
                submitAsync {
                    screen.sendDos(
                        listOf(
                            "updateOption<->OrryxSkillHUD@dragWidth@%OrryxSkillHUD_canvasV\$background-up_width%",
                            "updateOption<->OrryxSkillHUD@dragHeight@%OrryxSkillHUD_canvasV\$background-up_height%+%OrryxSkillHUD_canvasV\$background-center_height%+%OrryxSkillHUD_canvasV\$background-down_height%"
                        )
                    )
                }
            }
        }
    }

    override fun open() {
        remove(true)
        screen = GermGuiScreen.getGermGuiScreen("OrryxSkillHUD",
            skillHUDConfiguration
        )
        update()
        screen.openHud(viewer)
        GermPluginSkillHud.Companion.germSkillHudMap.getOrPut(owner.uniqueId) { hashMapOf() }[viewer.uniqueId] = this
    }

    override fun close() {
        remove(true)
    }

    protected open fun remove(close: Boolean = true) {
        germSkillHudMap.forEach {
            val iterator = it.value.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next.key == viewer.uniqueId) {
                    iterator.remove()
                    if (close) {
                        next.value.screen.close()
                    }
                }
            }
        }
    }
}