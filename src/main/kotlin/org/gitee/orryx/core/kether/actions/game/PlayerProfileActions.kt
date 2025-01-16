package org.gitee.orryx.core.kether.actions.game

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.profile.PlayerProfileManager.orryxProfile
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.KetherParser

object PlayerProfileActions {

    @KetherParser(["superBody"], namespace = NAMESPACE, shared = true)
    fun superBody() = combinationParser(
        Action.new("superBody", true)
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
                    "set", "to" -> foreach {
                        it.player.orryxProfile().setSuperBody(timeout)
                    }
                    "add", "+" -> foreach {
                        it.player.orryxProfile().addSuperBody(timeout)
                    }
                    "reduce", "-" -> foreach {
                        it.player.orryxProfile().reduceSuperBody(timeout)
                    }
                    "cancel", "stop" -> foreach {
                        it.player.orryxProfile().cancelSuperBody()
                    }
                }
            }
        }
    }


}