package org.gitee.orryx.core.ui.dragoncore

import eos.moe.dragoncore.api.event.KeyPressEvent
import eos.moe.dragoncore.api.event.KeyReleaseEvent
import eos.moe.dragoncore.api.gui.event.CustomPacketEvent
import eos.moe.dragoncore.network.PacketSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.common.keyregister.IKeyRegister
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.ui.ISkillHud
import org.gitee.orryx.core.ui.ISkillUI
import org.gitee.orryx.core.ui.IUIManager
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.getDataFolder
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

        registerBukkitListener(KeyPressEvent::class.java) { e ->
            e.player.keyPress(e.key, setting.castType === IKeyRegister.ActionType.PRESS)
        }

        registerBukkitListener(KeyReleaseEvent::class.java) { e ->
            e.player.keyRelease(e.key, setting.castType === IKeyRegister.ActionType.RELEASE)
        }

        registerBukkitListener(CustomPacketEvent::class.java) { e ->
            when(e.identifier) {
                "DragonCore" -> {
                    if (e.data.size == 1 && e.data[0] == "cache_loaded") {
                        PacketSender.sendYaml(e.player, "OrryxSkillUI.yml", YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/dragoncore/OrryxSkillUI.yml")))
                        PacketSender.sendYaml(e.player, "OrryxSkillHUD.yml", YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/dragoncore/OrryxSkillHUD.yml")))
                        if (setting.joinOpenHud) {
                            PacketSender.sendOpenHud(e.player, "OrryxSkillHUD")
                        }
                    }
                }
                "OrryxBindSkill" -> {
                    if (e.data.size == 3) {
                        val group = e.data[0]
                        val bindKey = e.data[1]
                        val skill = e.data[2]

                        e.player.job { job ->
                            BindKeyLoaderManager.getGroup(group)?.let { group ->
                                BindKeyLoaderManager.getBindKey(bindKey)?.let { bindKey ->
                                    e.player.getSkill(skill)?.let { skill ->
                                        job.setBindKey(skill, group,bindKey)
                                    }
                                }
                            }
                        }
                        IUIManager.INSTANCE.getSkillHUD(e.player)?.apply {
                            update()
                        }
                    }
                }
                "OrryxUnBindSkill" -> {
                    if (e.data.size == 2) {
                        val group = e.data[0]
                        val skill = e.data[1]

                        e.player.job { job ->
                            BindKeyLoaderManager.getGroup(group)?.let { group ->
                                e.player.getSkill(skill)?.let { skill ->
                                    job.unBindKey(skill, group)
                                }
                            }
                        }
                        IUIManager.INSTANCE.getSkillHUD(e.player)?.apply {
                            update()
                        }
                    }
                }
            }
        }

        registerBukkitListener(PlayerQuitEvent::class.java) { e ->
            DragonCoreSkillHud.getViewerHud(e.player)?.close()
        }
    }

    override fun getSkillHUD(viewer: Player): ISkillHud? {
        TODO("Not yet implemented")
    }

    override fun createSkillHUD(viewer: Player, owner: Player): ISkillHud {
        TODO("Not yet implemented")
    }

    override fun createSkillUI(viewer: Player, owner: Player): ISkillUI {
        TODO("Not yet implemented")
    }

    override fun reload() {
        config.reload()
        setting = Setting(config)
    }

}