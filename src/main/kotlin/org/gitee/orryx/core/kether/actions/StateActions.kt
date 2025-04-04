package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.KetherParser


object StateActions {

    @KetherParser(["next"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionNext() = combinationParser(
        Action.new("State状态机", "过渡到下一状态", "next", true)
            .description("过渡到下一状态")
            .addEntry("状态名", Type.STRING, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            bool(),
            theyContainer(true)
        ).apply(it) { isSprint, container ->
            future {
                ensureSync {
                    container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                        player.getSource().isSprinting = isSprint
                    }
                }
            }
        }
    }

}