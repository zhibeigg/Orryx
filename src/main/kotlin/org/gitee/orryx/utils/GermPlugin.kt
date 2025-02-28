package org.gitee.orryx.utils

import com.germ.germplugin.api.dynamic.gui.*
import org.bukkit.entity.Player

@Suppress("Unchecked_cast")
fun <T: GermGuiPart<*>> GermGuiScreen.pickPart(indexName: String): T {
    return getGuiPart(indexName) as T
}

@Suppress("Unchecked_cast")
fun <T: GermGuiPart<*>> GermGuiScroll.pickPart(indexName: String): T {
    return getGuiPart(indexName) as T
}

@Suppress("Unchecked_cast")
fun <T: GermGuiPart<*>> GermGuiCanvas.pickPart(indexName: String): T {
    return getGuiPart(indexName) as T
}

fun <E : Enum<*>, T : GermGuiPart<out GermGuiPart<*>>> IGuiPartCallback<E, T>.callback(vararg trigger: E, func: (player: Player, part: T) -> Unit) {
    registerCallbackHandler(
        { player, part ->
            func(player, part)
        },
        *trigger
    )
}