package org.gitee.orryx.core.kether.actions.compat.arcartx

import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.scriptParser
import taboolib.module.kether.KetherParser
import taboolib.module.kether.expects
import taboolib.module.kether.switch

object ArcartXActions {

    // @KetherParser(["arcartx", "ax"], namespace = ORRYX_NAMESPACE, shared = true)
    // private fun arcartX() = scriptParser(
    //     Action.new("ArcartX附属语句", "设置临时时装", "arcartx", true)
    //         .description("设置临时时装")
    //         .addEntry("armourers标识符", Type.SYMBOL, false, head = "armourers")
    //         .addContainerEntry(optional = true, default = "@self")
    // ) {
    //     it.switch {
    //         case("armourers") {
    //             when (it.expects("send", "clear", "update")) {
    //                 else -> error("AX armourers书写错误")
    //             }
    //         }
    //     }
    // }
}