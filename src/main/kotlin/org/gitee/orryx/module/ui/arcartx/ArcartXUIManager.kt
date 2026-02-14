package org.gitee.orryx.module.ui.arcartx

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.common.keyregister.IKeyRegister
import org.gitee.orryx.module.ui.ISkillHud
import org.gitee.orryx.module.ui.ISkillUI
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.utils.*
import priv.seventeen.artist.arcartx.api.ArcartXAPI
import priv.seventeen.artist.arcartx.event.client.ClientChannelEvent
import priv.seventeen.artist.arcartx.event.client.ClientCustomPacketEvent
import priv.seventeen.artist.arcartx.event.client.ClientKeyPressEvent
import priv.seventeen.artist.arcartx.event.client.ClientKeyReleaseEvent
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.releaseResourceFile
import taboolib.module.configuration.Configuration
import java.io.File

class ArcartXUIManager: IUIManager {

    override val config: Configuration = loadFromFile("ui/arcartx/setting.yml")
    private var setting = Setting(config)

    class Setting(config: Configuration) {

        val castType: IKeyRegister.ActionType = IKeyRegister.ActionType.valueOf(config.getString("ActionType", "press")!!.uppercase())

        val joinOpenHud: Boolean = config.getBoolean("JoinOpenHud", true)
    }

    init {
        releaseResourceFile("ui/arcartx/OrryxSkillUI.yml")
        releaseResourceFile("ui/arcartx/OrryxSkillHUD.yml")

        ArcartXSkillHud.skillHudConfiguration = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/arcartx/OrryxSkillHUD.yml"))
        ArcartXSkillUI.skillUIConfiguration = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/arcartx/OrryxSkillUI.yml"))

        val uiRegistry = ArcartXAPI.getUIRegistry()
        uiRegistry.register("OrryxSkillUI", ArcartXSkillUI.skillUIConfiguration)
        uiRegistry.register("OrryxSkillHUD", ArcartXSkillHud.skillHudConfiguration)

        registerBukkitListener(ClientKeyPressEvent::class.java) { e ->
            e.player.keyPress(e.keyName.uppercase(), setting.castType == IKeyRegister.ActionType.PRESS)
        }

        registerBukkitListener(ClientKeyReleaseEvent::class.java) { e ->
            e.player.keyRelease(e.keyName.uppercase(), setting.castType == IKeyRegister.ActionType.RELEASE)
        }

        registerBukkitListener(ClientChannelEvent::class.java) { e ->
            if (setting.joinOpenHud) {
                e.player.orryxProfileTo {
                    if (it.job != null) createSkillHUD(e.player, e.player).open()
                }
            }
        }

        registerBukkitListener(ClientCustomPacketEvent::class.java) { e ->
            when (e.id) {
                "OrryxOpen" -> {
                    ArcartXSkillUI(e.player, e.player).open()
                }
                "OrryxUpdateHUD" -> {
                    ArcartXSkillHud(e.player, e.player).update()
                }
                "OrryxSelectSkill" -> {
                    if (e.data.size >= 2) {
                        val owner = Bukkit.getPlayer(e.data[0].parseUUID() ?: return@registerBukkitListener) ?: return@registerBukkitListener
                        val skill = e.data[1]
                        if (e.player == owner || e.player.isOp) {
                            ArcartXSkillUI.sendDescription(e.player, owner, skill)
                        }
                    }
                }
                "OrryxBindSkill" -> {
                    if (e.data.size >= 4) {
                        val owner = Bukkit.getPlayer(e.data[0].parseUUID() ?: return@registerBukkitListener) ?: return@registerBukkitListener
                        val group = e.data[1]
                        val bindKey = e.data[2]
                        val skill = e.data[3]
                        if (e.player == owner || e.player.isOp) {
                            ArcartXSkillUI.bindSkill(e.player, owner, group, bindKey, skill)
                        }
                    }
                }
                "OrryxUnBindSkill" -> {
                    if (e.data.size >= 3) {
                        val owner = Bukkit.getPlayer(e.data[0].parseUUID() ?: return@registerBukkitListener) ?: return@registerBukkitListener
                        val group = e.data[1]
                        val skill = e.data[2]
                        if (e.player == owner || e.player.isOp) {
                            ArcartXSkillUI.unBindSkill(e.player, owner, group, skill)
                        }
                    }
                }
                "OrryxUpgradeSkill" -> {
                    if (e.data.size >= 2) {
                        val owner = Bukkit.getPlayer(e.data[0].parseUUID() ?: return@registerBukkitListener) ?: return@registerBukkitListener
                        val skill = e.data[1]
                        ArcartXSkillUI.upgrade(e.player, owner, skill)
                    }
                }
                "OrryxDowngradeSkill" -> {
                    if (e.data.size >= 2) {
                        val owner = Bukkit.getPlayer(e.data[0].parseUUID() ?: return@registerBukkitListener) ?: return@registerBukkitListener
                        val skill = e.data[1]
                        ArcartXSkillUI.downgrade(e.player, owner, skill)
                    }
                }
                "OrryxResetSkill" -> {
                    if (e.data.size >= 2) {
                        val owner = Bukkit.getPlayer(e.data[0].parseUUID() ?: return@registerBukkitListener) ?: return@registerBukkitListener
                        val skill = e.data[1]
                        ArcartXSkillUI.clearAndBackPoint(e.player, owner, skill)
                    }
                }
            }
        }

        registerBukkitListener(PlayerQuitEvent::class.java) { e ->
            ArcartXSkillHud.getViewerHud(e.player)?.close()
        }
    }

    override fun createSkillUI(viewer: Player, owner: Player): ISkillUI {
        return ArcartXSkillUI(viewer, owner)
    }

    override fun createSkillHUD(viewer: Player, owner: Player): ISkillHud {
        return ArcartXSkillHud(viewer, owner)
    }

    override fun getSkillHUD(viewer: Player): ISkillHud? {
        return ArcartXSkillHud.getViewerHud(viewer)
    }

    override fun reload() {
        config.reload()
        setting = Setting(config)
    }
}
