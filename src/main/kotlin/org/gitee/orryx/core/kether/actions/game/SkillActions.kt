package org.gitee.orryx.core.kether.actions.game

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.KetherParser

object SkillActions {

    @KetherParser(["ghost"], namespace = NAMESPACE, shared = true)
    fun actionGhost() = combinationParser(
        Action.new("Orryx Mod额外功能", "设置鬼影状态", "ghost", true)
            .description("设置鬼影状态")
            .addEntry("时长", Type.LONG, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            long(),
            theyContainer(true)
        ).apply(it) { timeout, container ->
            now {
                container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                    PluginMessageHandler.applyGhostEffect(player.player, timeout*50)
                }
            }
        }
    }

}