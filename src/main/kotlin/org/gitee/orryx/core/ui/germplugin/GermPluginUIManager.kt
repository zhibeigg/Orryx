package org.gitee.orryx.core.ui.germplugin

import org.bukkit.entity.Player
import org.gitee.orryx.core.ui.ISkillHud
import org.gitee.orryx.core.ui.ISkillUI
import org.gitee.orryx.core.ui.IUIManager
import org.gitee.orryx.utils.loadFromFile
import taboolib.common.platform.function.releaseResourceFile
import taboolib.module.configuration.Configuration

class GermPluginUIManager: IUIManager {

    override val config: Configuration = loadFromFile("ui/germplugin/setting.yml")

    init {
        releaseResourceFile("ui/germplugin/OrryxSkillUI.yml")
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

}