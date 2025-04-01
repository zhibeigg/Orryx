package org.gitee.orryx.core.ui.germplugin

import com.germ.germplugin.api.event.GermClientLinkedEvent
import com.germ.germplugin.api.event.GermKeyDownEvent
import com.germ.germplugin.api.event.GermKeyUpEvent
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.common.keyregister.IKeyRegister
import org.gitee.orryx.core.ui.ISkillHud
import org.gitee.orryx.core.ui.ISkillUI
import org.gitee.orryx.core.ui.IUIManager
import org.gitee.orryx.utils.keyPress
import org.gitee.orryx.utils.keyRelease
import org.gitee.orryx.utils.loadFromFile
import org.gitee.orryx.utils.orryxProfile
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.releaseResourceFile
import taboolib.module.configuration.Configuration
import java.io.File

class GermPluginUIManager: IUIManager {

    override val config: Configuration = loadFromFile("ui/germplugin/setting.yml")
    private var setting = Setting(config)

    class Setting(config: Configuration) {

        val castType: IKeyRegister.ActionType = IKeyRegister.ActionType.valueOf(config.getString("ActionType", "press")!!.uppercase())

        val joinOpenHud: Boolean = config.getBoolean("JoinOpenHud", true)

    }

    init {
        releaseResourceFile("ui/germplugin/OrryxSkillUI.yml")
        releaseResourceFile("ui/germplugin/OrryxSkillHUD.yml")
        GermPluginSkillHud.skillHUDConfiguration = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/germplugin/OrryxSkillHUD.yml"))
        GermPluginSkillUI.skillUIConfiguration = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/germplugin/OrryxSkillHUD.yml"))

        registerBukkitListener(GermKeyDownEvent::class.java, EventPriority.MONITOR) { e ->
            if (e.isCancelled) return@registerBukkitListener
            e.player.keyPress(e.keyType.simpleKey, setting.castType === IKeyRegister.ActionType.PRESS)
        }

        registerBukkitListener(GermKeyUpEvent::class.java, EventPriority.MONITOR) { e ->
            if (e.isCancelled) return@registerBukkitListener
            e.player.keyRelease(e.keyType.simpleKey, setting.castType === IKeyRegister.ActionType.RELEASE)
        }

        registerBukkitListener(GermClientLinkedEvent::class.java) { e ->
            if (setting.joinOpenHud) {
                e.player.orryxProfile {
                    if (it.job != null) createSkillHUD(e.player, e.player).open()
                }
            }
        }

        registerBukkitListener(PlayerQuitEvent::class.java) { e ->
            GermPluginSkillHud.getViewerHud(e.player)?.close()
        }
    }

    override fun createSkillUI(viewer: Player, owner: Player): ISkillUI {
        return GermPluginSkillUI(viewer, owner)
    }

    override fun createSkillHUD(viewer: Player, owner: Player): ISkillHud {
        return GermPluginSkillHud(viewer, owner)
    }

    override fun getSkillHUD(viewer: Player): ISkillHud? {
        return GermPluginSkillHud.getViewerHud(viewer)
    }

    override fun reload() {
        config.reload()
        setting = Setting(config)
    }

}