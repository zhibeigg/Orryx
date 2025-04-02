package org.gitee.orryx.core.kether.actions.game

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
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
            .addEntry("设置方法set/to,add/+,reduce/-,cancel/stop", Type.STRING, false)
            .addEntry("霸体时长", Type.LONG, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            text(),
            long(),
            theyContainer(true)
        ).apply(it) { type, timeout, container ->
            future {
                fun foreach(func: (player: PlayerTarget) -> Unit) {
                    container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                        func(player)
                    }
                }
                ensureSync {
                    when (type) {
                        "set", "to" -> foreach { target ->
                            target.getSource().orryxProfile { profile ->
                                profile.setSuperBody(timeout)
                            }
                        }

                        "add", "+" -> foreach { target ->
                            target.getSource().orryxProfile { profile ->
                                profile.addSuperBody(timeout)
                            }
                        }

                        "reduce", "-" -> foreach { target ->
                            target.getSource().orryxProfile { profile ->
                                profile.reduceSuperBody(timeout)
                            }
                        }

                        "cancel", "stop" -> foreach { target ->
                            target.getSource().orryxProfile { profile ->
                                profile.cancelSuperBody()
                            }
                        }
                    }
                }
            }
        }
    }


}