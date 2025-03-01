package org.gitee.orryx.core.ui.dragoncore

import eos.moe.dragoncore.api.event.KeyPressEvent
import eos.moe.dragoncore.api.event.KeyReleaseEvent
import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.IKeyRegister
import org.gitee.orryx.core.ui.ISkillHud
import org.gitee.orryx.core.ui.ISkillUI
import org.gitee.orryx.core.ui.IUIManager
import org.gitee.orryx.utils.keyPress
import org.gitee.orryx.utils.keyRelease
import org.gitee.orryx.utils.loadFromFile
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.releaseResourceFile
import taboolib.module.configuration.Configuration

class DragonCoreUIManager: IUIManager {

    override val config: Configuration = loadFromFile("ui/dragoncore/setting.yml")

    private val castType: IKeyRegister.ActionType
        get() = IKeyRegister.ActionType.valueOf(config.getString("ActionType", "press")!!.uppercase())

    init {
        releaseResourceFile("ui/dragoncore/OrryxSkillUI.yml")
        releaseResourceFile("ui/dragoncore/OrryxSkillHUD.yml")

        registerBukkitListener(KeyPressEvent::class.java) { e ->
            e.player.keyPress(e.key, castType === IKeyRegister.ActionType.PRESS)
        }

        registerBukkitListener(KeyReleaseEvent::class.java) { e ->
            e.player.keyRelease(e.key, castType === IKeyRegister.ActionType.RELEASE)
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

}