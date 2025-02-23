package org.gitee.orryx.core.ui

import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.ui.bukkit.BukkitUIManager
import taboolib.common.platform.function.info
import taboolib.module.chat.colored
import taboolib.module.configuration.Configuration

interface IUIManager {

    companion object {

        private val type
            get() = OrryxAPI.config.getString("UI.use", "bukkit")!!.uppercase()

        internal val INSTANCE: IUIManager =
            when (type) {
                "BUKKIT" -> {
                    info(("&e┣&7已选择原版UI &a√").colored())
                    BukkitUIManager()
                }

                "DRAGONCORE" -> {
                    info(("&e┣&7已选择龙核UI &a√").colored())
                    TODO()
                }

                "GERMPLUGIN", "GERM" -> {
                    info(("&e┣&7已选择萌芽UI &a√").colored())
                    TODO()
                }

                else -> error("未知的UI类型: $type")
            }

        @Reload(1)
        private fun reload() {
            INSTANCE.config.reload()
        }

    }

    val config: Configuration

    fun getSkillUI(viewer: Player, owner: Player): ISkillUI

}