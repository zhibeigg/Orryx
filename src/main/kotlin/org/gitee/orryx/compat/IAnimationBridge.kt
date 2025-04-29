package org.gitee.orryx.compat

import org.bukkit.entity.Player
import org.gitee.orryx.compat.dragoncore.DragonCoreAnimationBridge
import org.gitee.orryx.compat.germplugin.GermPluginAnimationBridge
import org.gitee.orryx.utils.DragonCorePlugin
import org.gitee.orryx.utils.GermPluginPlugin
import taboolib.common.util.unsafeLazy

interface IAnimationBridge {

    companion object {

        val INSTANCE by unsafeLazy {
            when {
                DragonCorePlugin.isEnabled -> DragonCoreAnimationBridge()
                GermPluginPlugin.isEnabled -> GermPluginAnimationBridge()
                else -> DefaultAnimationBridge()
            }
        }

    }

    fun setPlayerAnimation(viewer: Player, player: Player, animation: String, speed: Float)

    fun removePlayerAnimation(viewer: Player, player: Player, animation: String)

    fun clearPlayerAnimation(viewer: Player, player: Player)
}