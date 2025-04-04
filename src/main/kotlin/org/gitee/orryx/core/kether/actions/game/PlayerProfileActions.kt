package org.gitee.orryx.core.kether.actions.game

import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.kether.*

object PlayerProfileActions {

    @SubscribeEvent
    private fun drop(e: EntityDamageEvent) {
        val player = e.entity as? Player ?: return
        if (e.cause == EntityDamageEvent.DamageCause.FALL || e.cause == EntityDamageEvent.DamageCause.SUFFOCATION) {
            if (Orryx.api().profileAPI.isSuperBody(player) || Orryx.api().profileAPI.isSuperFoot(player)) {
                e.isCancelled = true
            }
        }
    }

    @KetherParser(["superBody"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun superBody() = scriptParser(
        arrayOf(
            Action.new("Orryx Profile玩家信息", "设置霸体状态", "superBody", true)
                .description("设置霸体状态")
                .addEntry("设置方法set/to,add/+,reduce/-,cancel/stop", Type.STRING, false)
                .addEntry("霸体时长", Type.LONG, false)
                .addContainerEntry(optional = true, default = "@self")
        )
    ) {
        it.switch {
            case("set", "to") {
                val timeout = nextParsedAction()
                val they = nextTheyContainerOrNull()
                actionTake {
                    ensureSync {
                        run(timeout).long { timeout ->
                            containerOrSelf(they) { container ->
                                container.forEachInstance<PlayerTarget> { target ->
                                    Orryx.api().profileAPI.setSuperBody(target.getSource(), timeout * 50)
                                }
                            }
                        }
                    }
                }
            }
            case("add", "+") {
                val timeout = nextParsedAction()
                val they = nextTheyContainerOrNull()
                actionTake {
                    ensureSync {
                        run(timeout).long { timeout ->
                            containerOrSelf(they) { container ->
                                container.forEachInstance<PlayerTarget> { target ->
                                    Orryx.api().profileAPI.addSuperBody(target.getSource(), timeout * 50)
                                }
                            }
                        }
                    }
                }
            }
            case("reduce", "-") {
                val timeout = nextParsedAction()
                val they = nextTheyContainerOrNull()
                actionTake {
                    ensureSync {
                        run(timeout).long { timeout ->
                            containerOrSelf(they) { container ->
                                container.forEachInstance<PlayerTarget> { target ->
                                    Orryx.api().profileAPI.reduceSuperBody(target.getSource(), timeout * 50)
                                }
                            }
                        }
                    }
                }
            }
            case("cancel", "stop") {
                val they = nextTheyContainerOrNull()
                actionTake {
                    ensureSync {
                        containerOrSelf(they) { container ->
                            container.forEachInstance<PlayerTarget> { target ->
                                Orryx.api().profileAPI.cancelSuperBody(target.getSource())
                            }
                        }
                    }
                }
            }
        }
    }

    @KetherParser(["superFoot"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun superFoot() = scriptParser(
        arrayOf(
            Action.new("Orryx Profile玩家信息", "设置免疫摔落状态", "superFoot", true)
                .description("设置免疫摔落状态")
                .addEntry("设置方法set/to,add/+,reduce/-,cancel/stop", Type.STRING, false)
                .addEntry("免疫时长", Type.LONG, false)
                .addContainerEntry(optional = true, default = "@self")
        )
    ) {
        it.switch {
            case("set", "to") {
                val timeout = nextParsedAction()
                val they = nextTheyContainerOrNull()
                actionTake {
                    ensureSync {
                        run(timeout).long { timeout ->
                            containerOrSelf(they) { container ->
                                container.forEachInstance<PlayerTarget> { target ->
                                    Orryx.api().profileAPI.setSuperFoot(target.getSource(), timeout * 50)
                                }
                            }
                        }
                    }
                }
            }
            case("add", "+") {
                val timeout = nextParsedAction()
                val they = nextTheyContainerOrNull()
                actionTake {
                    ensureSync {
                        run(timeout).long { timeout ->
                            containerOrSelf(they) { container ->
                                container.forEachInstance<PlayerTarget> { target ->
                                    Orryx.api().profileAPI.addSuperFoot(target.getSource(), timeout * 50)
                                }
                            }
                        }
                    }
                }
            }
            case("reduce", "-") {
                val timeout = nextParsedAction()
                val they = nextTheyContainerOrNull()
                actionTake {
                    ensureSync {
                        run(timeout).long { timeout ->
                            containerOrSelf(they) { container ->
                                container.forEachInstance<PlayerTarget> { target ->
                                    Orryx.api().profileAPI.reduceSuperFoot(target.getSource(), timeout * 50)
                                }
                            }
                        }
                    }
                }
            }
            case("cancel", "stop") {
                val they = nextTheyContainerOrNull()
                actionTake {
                    ensureSync {
                        containerOrSelf(they) { container ->
                            container.forEachInstance<PlayerTarget> { target ->
                                Orryx.api().profileAPI.cancelSuperFoot(target.getSource())
                            }
                        }
                    }
                }
            }
        }
    }

}