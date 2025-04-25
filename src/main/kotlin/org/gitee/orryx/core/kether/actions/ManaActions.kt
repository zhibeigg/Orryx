package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.*

object ManaActions {

    @KetherParser(["mana"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionMana() = scriptParser(
        Action.new("Mana法力", "检测法力", "mana", true)
            .description("检测是否有足够法力值")
            .addEntry("has", type = Type.SYMBOL, head = "has")
            .addEntry("检测法力值", type = Type.DOUBLE)
            .addContainerEntry("检测的目标", true, "@self")
            .result("是否有足够法力值", Type.BOOLEAN),
        Action.new("Mana法力", "给予法力", "mana")
            .description("玩家获得法力值")
            .addEntry("give", type = Type.SYMBOL, head = "give")
            .addEntry("法力值", type = Type.DOUBLE)
            .addContainerEntry("给予法力的目标", true, "@self"),
        Action.new("Mana法力", "减少法力", "mana")
            .description("获得法力值")
            .addEntry("take", type = Type.SYMBOL, head = "take")
            .addEntry("法力值", type = Type.DOUBLE)
            .addContainerEntry("减少法力的目标", true, "@self"),
        Action.new("Mana法力", "获取最大法力值", "mana")
            .description("获取玩家拥有的最大法力值")
            .addEntry("max", optional = true, type = Type.SYMBOL, head = "max")
            .addContainerEntry("获取的目标", true, "@self")
            .result("法力值", Type.DOUBLE),
        Action.new("Mana法力", "获取法力值", "mana")
            .description("获取玩家拥有的法力值")
            .addEntry("now", optional = true, type = Type.SYMBOL, head = "now")
            .addContainerEntry("获取的目标", true, "@self")
            .result("法力值", Type.DOUBLE)
    ) {
        it.switch {
            case("has") {
                val mana = it.nextParsedAction()
                val container = it.nextTheyContainerOrNull()
                actionFuture { complete ->
                    run(mana).double { mana ->
                        containerOrSelf(container) { container ->
                            val player = container.firstInstanceOrNull<PlayerTarget>()?.getSource()
                            if (player != null) {
                                IManaManager.INSTANCE.haveMana(player, mana).thenAccept { result ->
                                    complete.complete(result)
                                }
                            } else {
                                complete.complete(false)
                            }
                        }
                    }
                }
            }
            case("give") {
                val mana = it.nextParsedAction()
                val they = it.nextTheyContainerOrNull()
                actionNow {
                    run(mana).double { mana ->
                        containerOrSelf(they) { container ->
                            container.forEachInstance<PlayerTarget> { target ->
                                IManaManager.INSTANCE.giveMana(target.getSource(), mana)
                            }
                        }
                    }
                }
            }
            case("take") {
                val mana = it.nextParsedAction()
                val they = it.nextTheyContainerOrNull()
                actionNow {
                    run(mana).double { mana ->
                        containerOrSelf(they) { container ->
                            container.forEachInstance<PlayerTarget> { target ->
                                IManaManager.INSTANCE.takeMana(target.getSource(), mana)
                            }
                        }
                    }
                }
            }
            case("max") {
                val they = it.nextTheyContainerOrNull()
                actionFuture { future ->
                    containerOrSelf(they) { container ->
                        val player = container.firstInstanceOrNull<PlayerTarget>()?.getSource()
                        if (player != null) {
                            IManaManager.INSTANCE.getMaxMana(player).thenAccept { mana ->
                                future.complete(mana)
                            }
                        } else {
                            future.complete(0.0)
                        }
                    }
                }
            }
            case("now") {
                val they = it.nextTheyContainerOrNull()
                actionFuture { future ->
                    containerOrSelf(they) { container ->
                        val player = container.firstInstanceOrNull<PlayerTarget>()?.getSource()
                        if (player != null) {
                            IManaManager.INSTANCE.getMana(player).thenAccept { mana ->
                                future.complete(mana)
                            }
                        } else {
                            future.complete(0.0)
                        }
                    }
                }
            }
            other {
                val they = it.nextTheyContainerOrNull()
                actionFuture { future ->
                    containerOrSelf(they) { container ->
                        val player = container.firstInstanceOrNull<PlayerTarget>()?.getSource()
                        if (player != null) {
                            IManaManager.INSTANCE.getMana(player).thenAccept { mana ->
                                future.complete(mana)
                            }
                        } else {
                            future.complete(0.0)
                        }
                    }
                }
            }
        }
    }
}