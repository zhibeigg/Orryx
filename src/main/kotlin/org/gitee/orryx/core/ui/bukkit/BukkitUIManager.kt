package org.gitee.orryx.core.ui.bukkit

import org.bukkit.entity.Player
import org.gitee.orryx.core.ui.ISkillHud
import org.gitee.orryx.core.ui.ISkillUI
import org.gitee.orryx.core.ui.IUIManager
import org.gitee.orryx.utils.loadFromFile
import taboolib.module.configuration.Configuration

class BukkitUIManager: IUIManager {

    override val config: Configuration = loadFromFile("bukkitUI.yml")

    override fun createSkillUI(viewer: Player, owner: Player): ISkillUI {
        return BukkitSkillUI(viewer, owner)
    }

    override fun createSkillHUD(viewer: Player, owner: Player): ISkillHud {
        return BukkitSkillHud(viewer, owner)
    }

    override fun getSkillHUD(viewer: Player): ISkillHud? {
        return BukkitSkillHud.getViewerHud(viewer)
    }

}