package org.gitee.orryx.core.key

import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.IKeyRegister
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.utils.tryCast
import taboolib.library.configuration.ConfigurationSection

class BindKeyLoader(override val key: String, val configurationSection: ConfigurationSection): IBindKey {

    override val sort: Int = configurationSection.getInt("sort")

    override val category: String? = configurationSection.getString("category")

    override val defaultKey: String? = configurationSection.getString("default")

    override fun checkAndCast(player: Player, key: List<String>, timeout: Long, actionType: IKeyRegister.ActionType, sort: Boolean): Boolean {
        val boolean = KeyRegisterManager.getKeyRegister(player.uniqueId)?.isKeysInTimeout(key, timeout, actionType, sort) ?: false
        if (boolean) tryCast(player)
        return boolean
    }

    override fun toString(): String {
        return "BindKeyLoader(key=$key, sort=$sort)"
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is BindKeyLoader) {
            key == other.key
        } else {
            false
        }
    }
}