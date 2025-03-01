package org.gitee.orryx.utils

import com.germ.germplugin.api.dynamic.DynamicBase
import com.germ.germplugin.api.dynamic.gui.*
import org.bukkit.entity.Player

inline fun <reified T: GermGuiPart<out DynamicBase>> GermGuiScreen.pickPart(indexName: String): T {
    return getGuiPart(indexName) as T
}

inline fun <reified T: GermGuiPart<out DynamicBase>> GermGuiScroll.pickPart(indexName: String): T {
    return getGuiPart(indexName) as T
}

inline fun <reified T: GermGuiPart<out DynamicBase>> GermGuiCanvas.pickPart(indexName: String): T {
    return getGuiPart(indexName) as T
}

inline fun <E : Enum<*>, reified T : GermGuiPart<out DynamicBase>> IGuiPartCallback<E, T>.callback(vararg trigger: E, crossinline func: (player: Player, part: T) -> Unit) {
    registerCallbackHandler(
        { player, part ->
            func(player, part)
        },
        *trigger
    )
}