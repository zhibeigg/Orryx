package org.gitee.orryx.compat

import org.bukkit.entity.Player

class DefaultAnimationBridge: IAnimationBridge {

    override fun setPlayerAnimation(viewer: Player, player: Player, animation: String, speed: Float) {
        error("not supported")
    }

    override fun removePlayerAnimation(viewer: Player, player: Player, animation: String) {
        error("not supported")
    }

    override fun clearPlayerAnimation(viewer: Player, player: Player) {
        error("not supported")
    }

}