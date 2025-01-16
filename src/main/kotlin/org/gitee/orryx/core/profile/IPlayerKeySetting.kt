package org.gitee.orryx.core.profile

import org.bukkit.entity.Player

interface IPlayerKeySetting {

    companion object {

        internal var INSTANCE: IPlayerKeySetting = PlayerKeySetting()
            private set

        internal fun register(setting: IPlayerKeySetting) {
            INSTANCE = setting
        }

    }

    fun aimConfirmKey(player: Player? = null): String

    fun aimCancelKey(player: Player? = null): String

}