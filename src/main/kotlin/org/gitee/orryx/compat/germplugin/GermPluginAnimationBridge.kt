package org.gitee.orryx.compat.germplugin

import com.germ.germplugin.api.GermPacketAPI
import com.germ.germplugin.api.bean.AnimDataDTO
import org.bukkit.entity.Player
import org.gitee.orryx.compat.IAnimationBridge
import taboolib.common.platform.function.warning

class GermPluginAnimationBridge: IAnimationBridge {

    override fun setPlayerAnimation(viewer: Player, player: Player, animation: String, speed: Float) {
        GermPacketAPI.sendBendAction(viewer, player.entityId, AnimDataDTO(animation, speed, false))
    }

    override fun removePlayerAnimation(viewer: Player, player: Player, animation: String) {
        warning("GermPlugin not support removePlayerAnimation")
    }

    override fun clearPlayerAnimation(viewer: Player, player: Player) {
        GermPacketAPI.sendBendClear(viewer, player.entityId)
    }

}