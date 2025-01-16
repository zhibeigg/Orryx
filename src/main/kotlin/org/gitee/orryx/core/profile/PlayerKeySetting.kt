package org.gitee.orryx.core.profile

import org.bukkit.entity.Player

class PlayerKeySetting: IPlayerKeySetting {

    override fun aimCancelKey(player: Player?): String = "MOUSE_RIGHT"

    override fun aimConfirmKey(player: Player?): String = "MOUSE_LEFT"

}