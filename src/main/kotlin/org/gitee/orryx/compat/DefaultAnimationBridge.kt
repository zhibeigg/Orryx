package org.gitee.orryx.compat

import org.bukkit.entity.Player

class DefaultAnimationBridge: IAnimationBridge {

    override fun setPlayerAnimation(viewer: Player, player: Player, animation: String, speed: Float) = Unit

    override fun removePlayerAnimation(viewer: Player, player: Player, animation: String) = Unit

    override fun clearPlayerAnimation(viewer: Player, player: Player) = Unit

}