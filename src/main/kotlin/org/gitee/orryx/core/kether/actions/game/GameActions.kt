package org.gitee.orryx.core.kether.actions.game

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.KetherParser

object GameActions {

    @KetherParser(["sprint"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionSprint() = combinationParser(
        Action.new("Game原版游戏", "设置跑步状态", "sprint", true)
            .description("设置跑步状态")
            .addEntry("是否跑步", Type.BOOLEAN, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            bool(),
            theyContainer(true)
        ).apply(it) { isSprint, container ->
            now {
                container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                    player.player.isSprinting = isSprint
                }
            }
        }
    }

}