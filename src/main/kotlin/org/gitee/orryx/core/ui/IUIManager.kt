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

    /**
     * UI配置
     * */
    val config: Configuration

    /**
     * 创建技能UI
     * @param viewer 浏览者
     * @param owner 拥有者
     * @return 技能UI[ISkillUI]
     * */
    fun createSkillUI(viewer: Player, owner: Player): ISkillUI

    /**
     * 创建技能HUD
     * @param viewer 浏览者
     * @param owner 拥有者
     * @return 技能HUD[ISkillHud]
     * */
    fun createSkillHUD(viewer: Player, owner: Player): ISkillHud

    /**
     * 获取技能HUD
     * @param viewer 浏览者
     * @return 技能HUD[ISkillHud]
     * */
    fun getSkillHUD(viewer: Player): ISkillHud?

}