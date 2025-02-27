package org.gitee.orryx.core.ui.dragoncore

import org.bukkit.entity.Player
import org.gitee.orryx.core.ui.ISkillHud
import org.gitee.orryx.core.ui.ISkillUI
import org.gitee.orryx.core.ui.IUIManager
import org.gitee.orryx.utils.loadFromFile
import taboolib.module.configuration.Configuration

class DragonCoreUIManager: IUIManager {

    override val config: Configuration = loadFromFile("ui/dragoncore/setting.yml")

    override fun getSkillHUD(viewer: Player): ISkillHud? {
        TODO("Not yet implemented")
    }

    override fun createSkillHUD(viewer: Player, owner: Player): ISkillHud {
        TODO("Not yet implemented")
    }

    override fun createSkillUI(viewer: Player, owner: Player): ISkillUI {
        TODO("Not yet implemented")
    }


}