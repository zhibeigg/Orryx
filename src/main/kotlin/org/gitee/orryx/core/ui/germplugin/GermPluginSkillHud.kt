package org.gitee.orryx.core.ui.germplugin

import com.germ.germplugin.api.dynamic.gui.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.ui.AbstractSkillHud
import org.gitee.orryx.core.ui.IUIManager
import org.gitee.orryx.core.ui.IUIManager.Companion.skillCooldownMap
import org.gitee.orryx.utils.bindKeys
import org.gitee.orryx.utils.getBindSkills
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.pickPart
import taboolib.common.platform.function.getDataFolder
import java.io.File
import java.util.*
import kotlin.collections.set

open class GermPluginSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    companion object {

        /**
         * owner, viewer, [GermPluginSkillHud]
         */
        internal val germSkillHudMap by lazy { hashMapOf<UUID, MutableMap<UUID, GermPluginSkillHud>>() }

        fun getViewerHud(player: Player): GermPluginSkillHud? {
            return germSkillHudMap.firstNotNullOfOrNull {
                it.value[player.uniqueId]
            }
        }

        @Reload(2)
        private fun reload() {
            if (IUIManager.INSTANCE !is GermPluginUIManager) return
            germSkillHudMap.forEach {
                it.value.forEach { map ->
                    map.value.update()
                }
            }
        }

        private val skillHUDConfiguration
            get() = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/germplugin/OrryxSkillHUD.yml"))

    }

    protected open lateinit var screen: GermGuiScreen

    override fun update() {
        val skillBindCanvas = screen.pickPart<GermGuiCanvas>("skillBindCanvas")
        val bindKeyBackgroundBase = screen.pickPart<GermGuiTexture>("skillBindKeyBackground")
        val bindKeyLabelBase = screen.pickPart<GermGuiLabel>("skillBindKey")
        val bindKeyIconBase = screen.pickPart<GermGuiTexture>("skillBindKeyIcon")
        val bindKeyCooldownBase = screen.pickPart<GermGuiColor>("skillBindKeyCooldown")
        val bindKeyCooldownLabelBase = screen.pickPart<GermGuiLabel>("skillBindKeyCooldownLabel")

        val bind = owner.job()?.getBindSkills() ?: return
        val bindKeys = bindKeys()
        val width = StringBuilder()
        bindKeys.forEach {
            val bindKeyBackground = GermGuiTexture("skillBindKey-background-${it.key}").copyFrom(bindKeyBackgroundBase)
            val bindKeyLabel = GermGuiLabel("skillBindKey-label-${it.key}").copyFrom(bindKeyLabelBase)
            val bindKeyIcon = GermGuiTexture("skillBindKey-icon-${it.key}").copyFrom(bindKeyIconBase)
            val bindKeyCooldown = GermGuiColor("skillBindKey-cooldown-${it.key}").copyFrom(bindKeyCooldownBase)
            val bindKeyCooldownLabel = GermGuiLabel("skillBindKey-cooldown-label-${it.key}").copyFrom(bindKeyCooldownLabelBase)

            bindKeyBackground.enable = true
            bindKeyLabel.setText(it.key)
            bindKeyLabel.enable = true

            val canvas = GermGuiCanvas("skillBindKey-canvas-${it.key}")

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
        val canvas = screen.pickPart<GermGuiCanvas>("canvas")
        width.append("${skillBindCanvas.srcLayout.getString("gapX", "0")!!}*${bindKeys.size - 1}")
        skillBindCanvas.width = canvas.width
        skillBindCanvas.height = canvas.height
        canvas.pickPart<GermGuiTexture>("background-center").width = width.toString()
    }

    override fun open() {
        remove(true)
        screen = GermGuiScreen.getGermGuiScreen("OrryxSkillHUD", skillHUDConfiguration)
        update()
        screen.openHud(viewer)
        germSkillHudMap.getOrPut(owner.uniqueId) { hashMapOf() }[viewer.uniqueId] = this
    }

    override fun close() {
        remove()
        screen.close()
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