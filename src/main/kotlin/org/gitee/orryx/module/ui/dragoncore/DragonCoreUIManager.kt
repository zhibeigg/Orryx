package org.gitee.orryx.module.ui.dragoncore

import eos.moe.dragoncore.api.event.KeyPressEvent
import eos.moe.dragoncore.api.event.KeyReleaseEvent
import eos.moe.dragoncore.api.gui.event.CustomPacketEvent
import eos.moe.dragoncore.network.PacketSender
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.common.keyregister.IKeyRegister
import org.gitee.orryx.module.ui.ISkillHud
import org.gitee.orryx.module.ui.ISkillUI
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.info
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.releaseResourceFile
import taboolib.module.configuration.Configuration
import java.io.File

class DragonCoreUIManager: IUIManager {

    override val config: Configuration = loadFromFile("ui/dragoncore/setting.yml")
    private var setting = Setting(config)

    class Setting(config: Configuration) {

        val castType: IKeyRegister.ActionType = IKeyRegister.ActionType.valueOf(config.getString("ActionType", "press")!!.uppercase())

        val joinOpenHud: Boolean = config.getBoolean("JoinOpenHud", true)
    }

    init {
        releaseResourceFile("ui/dragoncore/OrryxSkillUI.yml")
        releaseResourceFile("ui/dragoncore/OrryxSkillHUD.yml")

        DragonCoreSkillHud.skillHudConfiguration = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/dragoncore/OrryxSkillHUD.yml"))
        DragonCoreSkillUI.skillUIConfiguration = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/dragoncore/OrryxSkillUI.yml"))

        registerBukkitListener(KeyPressEvent::class.java) { e ->
            if (e.isCancelled) return@registerBukkitListener
            e.player.keyPress(e.key, setting.castType === IKeyRegister.ActionType.PRESS)
        }

        registerBukkitListener(KeyReleaseEvent::class.java) { e ->
            e.player.keyRelease(e.key, setting.castType === IKeyRegister.ActionType.RELEASE)
        }

        registerBukkitListener(CustomPacketEvent::class.java) { e ->
            when(e.identifier) {
                "DragonCore" -> {
                    if (e.data.size == 1 && e.data[0] == "cache_loaded") {
                        PacketSender.sendYaml(e.player, "gui/OrryxSkillUI.yml", DragonCoreSkillUI.skillUIConfiguration)
                        PacketSender.sendYaml(e.player, "gui/OrryxSkillHUD.yml", DragonCoreSkillHud.skillHudConfiguration)
                        if (setting.joinOpenHud) {
                            e.player.orryxProfileTo {
                                if (it.job != null) createSkillHUD(e.player, e.player).open()
                            }
                        }
                    }
                }
                "OrryxOpen" -> {
                    DragonCoreSkillUI(e.player, e.player).open()
                }
                "OrryxSelectSkill" -> {
                    if (e.data.size == 2) {
                        val owner = Bukkit.getPlayer(e.data[0].parseUUID()!!) ?: return@registerBukkitListener
                        val skill = e.data[1]

                        if (e.player == owner || e.player.isOp) {
                            DragonCoreSkillUI.sendDescription(e.player, owner, skill)
                        }
                    }
                }
                "OrryxBindSkill" -> {
                    if (e.data.size == 4) {
                        val owner = Bukkit.getPlayer(e.data[0].parseUUID()!!) ?: return@registerBukkitListener
                        val group = e.data[1]
                        val bindKey = e.data[2]
                        val skill = e.data[3]

                        if (e.player == owner || e.player.isOp) {
                            DragonCoreSkillUI.bindSkill(e.player, owner, group, bindKey, skill)
                        }
                    }
                }
                "OrryxUnBindSkill" -> {
                    if (e.data.size == 3) {
                        val owner = Bukkit.getPlayer(e.data[0].parseUUID()!!) ?: return@registerBukkitListener
                        val group = e.data[1]
                        val skill = e.data[2]

                        DragonCoreSkillUI.unBindSkill(e.player, owner, group, skill)
                    }
                }
                "OrryxUpgradeSkill" -> {
                    if (e.data.size == 2) {
                        val owner = Bukkit.getPlayer(e.data[0].parseUUID()!!) ?: return@registerBukkitListener
                        val skill = e.data[1]

                        DragonCoreSkillUI.upgrade(e.player, owner, skill)
                    }
                }
            }
        }

        registerBukkitListener(PlayerQuitEvent::class.java) { e ->
            DragonCoreSkillHud.getViewerHud(e.player)?.close()
        }
    }

    override fun getSkillHUD(viewer: Player): ISkillHud? {
        return DragonCoreSkillHud.getViewerHud(viewer)
    }

    override fun createSkillHUD(viewer: Player, owner: Player): ISkillHud {
        return DragonCoreSkillHud(viewer, owner)
    }

    override fun createSkillUI(viewer: Player, owner: Player): ISkillUI {
        return DragonCoreSkillUI(viewer, owner)
    }

    override fun reload() {
        config.reload()
        setting = Setting(config)
    }
}