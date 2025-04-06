package org.gitee.orryx.core.kether.actions.game

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.*

object PlayerProfileActions {

    @KetherParser(["superBody"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun superBody() = scriptParser(
        arrayOf(
            Action.new("Orryx Profile玩家信息", "设置霸体状态", "superBody", true)
                .description("设置霸体状态")
                .addEntry("设置方法", Type.SYMBOL, false, head = "set/to/=,add/+,reduce/-,cancel/stop")
                .addEntry("霸体时长", Type.LONG, false)
                .addContainerEntry(optional = true, default = "@self"),
            Action.new("Orryx Profile玩家信息", "获取霸体状态倒计时", "superBody", true)
                .description("获取霸体状态倒计时")
                .addEntry("倒计时占位符", Type.STRING, false, head = "count")
                .addContainerEntry(optional = true, default = "@self")
                .result("倒计时Tick", Type.LONG)
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
            case("count") {
                val they = nextTheyContainerOrNull()
                actionFuture { f ->
                    ensureSync {
                        containerOrSelf(they) { container ->
                            f.complete(container.firstInstanceOrNull<PlayerTarget>()?.let { target -> Orryx.api().profileAPI.superBodyCountdown(target.getSource()) / 50 })
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
                .addEntry("设置方法", Type.SYMBOL, false, head = "set/to/=,add/+,reduce/-,cancel/stop")
                .addEntry("免疫时长", Type.LONG, false)
                .addContainerEntry(optional = true, default = "@self"),
            Action.new("Orryx Profile玩家信息", "获取免疫摔落状态倒计时", "superFoot", true)
                .description("获取免疫摔落状态倒计时")
                .addEntry("倒计时占位符", Type.STRING, false, head = "count")
                .addContainerEntry(optional = true, default = "@self")
                .result("倒计时Tick", Type.LONG)
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
            case("count") {
                val they = nextTheyContainerOrNull()
                actionFuture { f ->
                    ensureSync {
                        containerOrSelf(they) { container ->
                            f.complete(container.firstInstanceOrNull<PlayerTarget>()?.let { target -> Orryx.api().profileAPI.superFootCountdown(target.getSource()) / 50 })
                        }
                    }
                }
            }
        }
    }

    @KetherParser(["invincible"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun superInvincible() = scriptParser(
        arrayOf(
            Action.new("Orryx Profile玩家信息", "设置无敌状态", "invincible", true)
                .description("设置无敌状态")
                .addEntry("设置方法", Type.SYMBOL, false, head = "set/to/=,add/+,reduce/-,cancel/stop")
                .addEntry("无敌时长", Type.LONG, false)
                .addContainerEntry(optional = true, default = "@self"),
            Action.new("Orryx Profile玩家信息", "获取无敌状态倒计时", "invincible", true)
                .description("获取无敌状态倒计时")
                .addEntry("倒计时占位符", Type.STRING, false, head = "count")
                .addContainerEntry(optional = true, default = "@self")
                .result("倒计时Tick", Type.LONG)
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
                                    Orryx.api().profileAPI.setInvincible(target.getSource(), timeout * 50)
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
                                    Orryx.api().profileAPI.addInvincible(target.getSource(), timeout * 50)
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
                                    Orryx.api().profileAPI.reduceInvincible(target.getSource(), timeout * 50)
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
                                Orryx.api().profileAPI.cancelInvincible(target.getSource())
                            }
                        }
                    }
                }
            }
            case("count") {
                val they = nextTheyContainerOrNull()
                actionFuture { f ->
                    ensureSync {
                        containerOrSelf(they) { container ->
                            f.complete(container.firstInstanceOrNull<PlayerTarget>()?.let { target -> Orryx.api().profileAPI.invincibleCountdown(target.getSource()) / 50 })
                        }
                    }
                }
            }
        }
    }

    @KetherParser(["silence"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun superSilence() = scriptParser(
        arrayOf(
            Action.new("Orryx Profile玩家信息", "设置沉默状态", "silence", true)
                .description("设置沉默状态")
                .addEntry("设置方法", Type.SYMBOL, false, head = "set/to/=,add/+,reduce/-,cancel/stop")
                .addEntry("沉默时长", Type.LONG, false)
                .addContainerEntry(optional = true, default = "@self"),
            Action.new("Orryx Profile玩家信息", "获取沉默状态倒计时", "silence", true)
                .description("获取沉默状态倒计时")
                .addEntry("倒计时占位符", Type.STRING, false, head = "count")
                .addContainerEntry(optional = true, default = "@self")
                .result("倒计时Tick", Type.LONG)
        )
    ) {
        it.switch {
            case("set", "to", "=") {
                val timeout = nextParsedAction()
                val they = nextTheyContainerOrNull()
                actionTake {
                    ensureSync {
                        run(timeout).long { timeout ->
                            containerOrSelf(they) { container ->
                                container.forEachInstance<PlayerTarget> { target ->
                                    Orryx.api().profileAPI.setSilence(target.getSource(), timeout * 50)
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
                                    Orryx.api().profileAPI.addSilence(target.getSource(), timeout * 50)
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
                                    Orryx.api().profileAPI.reduceSilence(target.getSource(), timeout * 50)
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
                                Orryx.api().profileAPI.cancelSilence(target.getSource())
                            }
                        }
                    }
                }
            }
            case("count") {
                val they = nextTheyContainerOrNull()
                actionFuture { f ->
                    ensureSync {
                        containerOrSelf(they) { container ->
                            f.complete(container.firstInstanceOrNull<PlayerTarget>()?.let { target -> Orryx.api().profileAPI.silenceCountdown(target.getSource()) / 50 })
                        }
                    }
                }
            }
        }
    }

}