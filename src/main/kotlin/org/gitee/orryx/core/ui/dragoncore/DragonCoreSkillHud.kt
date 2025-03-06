package org.gitee.orryx.core.ui.dragoncore

import eos.moe.dragoncore.network.PacketSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.ui.AbstractSkillHud
import org.gitee.orryx.core.ui.IUIManager
import org.gitee.orryx.core.ui.germplugin.GermPluginSkillHud
import taboolib.common.platform.function.getDataFolder
import taboolib.platform.util.onlinePlayers
import java.io.File
import java.util.*

class DragonCoreSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    companion object {

        /**
         * owner, viewer, [GermPluginSkillHud]
         */
        internal val dragonSkillHudMap by lazy { hashMapOf<UUID, MutableMap<UUID, DragonCoreSkillHud>>() }
        internal lateinit var skillHudConfiguration: YamlConfiguration

        @Reload(2)
        private fun reload() {
            if (IUIManager.INSTANCE !is DragonCoreUIManager) return
            skillHudConfiguration = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/dragoncore/OrryxSkillHUD.yml"))
            onlinePlayers.forEach {
                PacketSender.sendYaml(it, "gui/OrryxSkillHUD.yml", skillHudConfiguration)
                getViewerHud(it)?.update()
            }
        }

        fun getViewerHud(player: Player): DragonCoreSkillHud? {
            return dragonSkillHudMap.firstNotNullOfOrNull {
                it.value[player.uniqueId]
            }
        }

    }

    override fun update() {
        TODO("Not yet implemented")
    }

    override fun open() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}