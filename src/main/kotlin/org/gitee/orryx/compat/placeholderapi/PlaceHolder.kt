package org.gitee.orryx.compat.placeholderapi

import org.bukkit.entity.Player
import org.gitee.orryx.utils.eval
import taboolib.common.platform.function.console
import taboolib.module.kether.orNull
import taboolib.platform.compat.PlaceholderExpansion

object PlaceHolder: PlaceholderExpansion {

    override val identifier: String
        get() = "orryx"

    override fun onPlaceholderRequest(player: Player?, args: String): String {
        return player?.eval(args, emptyMap())?.orNull()?.toString() ?: console().eval(args, emptyMap()).orNull().toString()
    }

}