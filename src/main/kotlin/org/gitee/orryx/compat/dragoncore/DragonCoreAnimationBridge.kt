package org.gitee.orryx.compat.dragoncore

import org.bukkit.entity.Player
import org.gitee.orryx.compat.IAnimationBridge

class DragonCoreAnimationBridge: IAnimationBridge {

    override fun setPlayerAnimation(viewer: Player, player: Player, animation: String, speed: Float) {
        DragonCoreCustomPacketSender.setPlayerAnimation(viewer, player.uniqueId, animation, speed)
    }

    override fun removePlayerAnimation(viewer: Player, player: Player, animation: String) {
        DragonCoreCustomPacketSender.removePlayerAnimation(viewer, player.uniqueId, animation)
    }

    override fun clearPlayerAnimation(viewer: Player, player: Player) {
        DragonCoreCustomPacketSender.removePlayerAnimation(viewer, player.uniqueId)
    }
}