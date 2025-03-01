package org.gitee.orryx.core.key

import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.IKeyRegister
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.utils.tryCast
import taboolib.library.configuration.ConfigurationSection

class BindKeyLoader(override val key: String, val configurationSection: ConfigurationSection): IBindKey {

    val keys: List<String> = key.split("+")

    override val sort: Int
        get() = configurationSection.getInt("sort")

    override fun checkAndCast(player: Player, timeout: Long, actionType: IKeyRegister.ActionType, sort: Boolean): Boolean {
        val boolean = KeyRegisterManager.getKeyRegister(player.uniqueId)?.isKeysInTimeout(keys, timeout, actionType, sort) ?: false
        if (boolean) tryCast(player)
        return boolean
    }

}