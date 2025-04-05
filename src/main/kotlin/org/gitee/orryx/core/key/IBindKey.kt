package org.gitee.orryx.core.key

import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.IKeyRegister

interface IBindKey {

    val key: String

    val sort: Int

    fun checkAndCast(player: Player, key: List<String>, timeout: Long, actionType: IKeyRegister.ActionType, sort: Boolean): Boolean

}