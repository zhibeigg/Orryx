package org.gitee.orryx.core.kether.actions.game

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.profile.PlayerProfileManager.orryxProfile
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.KetherParser

object PlayerProfileActions {

    @KetherParser(["superBody"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun superBody() = combinationParser(
        Action.new("Orryx Profile玩家信息", "设置霸体状态", "superBody", true)
            .description("设置霸体状态")
            .addEntry("设置方法", Type.STRING, false)
            .addEntry("霸体时长", Type.LONG, true, "0")
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            text(),
            long().option().defaultsTo(0),
            theyContainer(true)
        ).apply(it) { type, timeout, container ->
            now {
                fun foreach(func: (player: PlayerTarget) -> Unit) {
                    container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                        func(player)
                    }
                }
                when (type) {
                    "set", "to" -> foreach { target ->
                        target.getSource().orryxProfile().setSuperBody(timeout)
                    }
                    "add", "+" -> foreach { target ->
                        target.getSource().orryxProfile().addSuperBody(timeout)
                    }
                    "reduce", "-" -> foreach { target ->
                        target.getSource().orryxProfile().reduceSuperBody(timeout)
                    }
                    "cancel", "stop" -> foreach { target ->
                        target.getSource().orryxProfile().cancelSuperBody()
                    }
                }
            }
        }
    }


}