package org.gitee.orryx.compat.arcartx

import org.bukkit.entity.Player
import org.gitee.orryx.compat.IAnimationBridge
import priv.seventeen.artist.arcartx.util.PlayerUtils.arcartXHandler

class ArcartXAnimationBridge: IAnimationBridge {

    override fun setPlayerAnimation(viewer: Player, player: Player, animation: String, speed: Float) {
        player.arcartXHandler?.playAnimation(animation, 1.0, 0, -1)
    }

    override fun removePlayerAnimation(viewer: Player, player: Player, animation: String) {
        player.arcartXHandler?.playAnimation("empty", 1.0, 0, -1)
    }

    override fun clearPlayerAnimation(viewer: Player, player: Player) {
        player.arcartXHandler?.playAnimation("empty", 1.0, 0, -1)
    }
}