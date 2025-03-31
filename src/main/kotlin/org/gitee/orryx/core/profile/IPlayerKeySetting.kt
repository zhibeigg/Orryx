package org.gitee.orryx.core.profile

import org.bukkit.entity.Player

interface IPlayerKeySetting {

    fun aimConfirmKey(player: Player? = null): String

    fun aimCancelKey(player: Player? = null): String

}