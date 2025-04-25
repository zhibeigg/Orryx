package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.spirit.ISpiritManager
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.*

object SpiritActions {

    @KetherParser(["spirit"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionSpirit() = scriptParser(
        Action.new("Spirit精力", "检测精力", "spirit", true)
            .description("检测是否有足够精力值")
            .addEntry("has", type = Type.SYMBOL, head = "has")
            .addEntry("检测精力值", type = Type.DOUBLE)
            .addContainerEntry("检测的目标", true, "@self")
            .result("是否有足够精力值", Type.BOOLEAN),
        Action.new("Spirit精力", "给予精力", "spirit", true)
            .description("玩家获得精力值")
            .addEntry("give", type = Type.SYMBOL, head = "give")
            .addEntry("精力值", type = Type.DOUBLE)
            .addContainerEntry("给予精力的目标", true, "@self"),
        Action.new("Spirit精力", "减少精力", "spirit", true)
            .description("获得精力值")
            .addEntry("take", type = Type.SYMBOL, head = "take")
            .addEntry("精力值", type = Type.DOUBLE)
            .addContainerEntry("减少精力的目标", true, "@self"),
        Action.new("Spirit精力", "获取最大精力值", "spirit", true)
            .description("获取玩家拥有的最大精力值")
            .addEntry("max", optional = true, type = Type.SYMBOL, head = "max")
            .addContainerEntry("获取的目标", true, "@self")
            .result("精力值", Type.DOUBLE),
        Action.new("Spirit精力", "获取精力值", "spirit", true)
            .description("获取玩家拥有的精力值")
            .addEntry("now", optional = true, type = Type.SYMBOL, head = "now")
            .addContainerEntry("获取的目标", true, "@self")
            .result("精力值", Type.DOUBLE)
    ) {
        it.switch {
            case("has") {
                val spirit = it.nextParsedAction()
                val container = it.nextTheyContainerOrNull()
                actionFuture { complete ->
                    run(spirit).double { spirit ->
                        containerOrSelf(container) { container ->
                            val player = container.firstInstanceOrNull<PlayerTarget>()?.getSource()
                            if (player != null) {
                                complete.complete(ISpiritManager.INSTANCE.haveSpirit(player, spirit))
                            } else {
                                complete.complete(false)
                            }
                        }
                    }
                }
            }
            case("give") {
                val spirit = it.nextParsedAction()
                val they = it.nextTheyContainerOrNull()
                actionNow {
                    run(spirit).double { spirit ->
                        containerOrSelf(they) { container ->
                            container.forEachInstance<PlayerTarget> { target ->
                                ISpiritManager.INSTANCE.giveSpirit(target.getSource(), spirit)
                            }
                        }
                    }
                }
            }
            case("take") {
                val spirit = it.nextParsedAction()
                val they = it.nextTheyContainerOrNull()
                actionNow {
                    run(spirit).double { spirit ->
                        containerOrSelf(they) { container ->
                            container.forEachInstance<PlayerTarget> { target ->
                                ISpiritManager.INSTANCE.takeSpirit(target.getSource(), spirit)
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
                            ISpiritManager.INSTANCE.getMaxSpirit(player).thenAccept { spirit ->
                                future.complete(spirit)
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
                            future.complete(ISpiritManager.INSTANCE.getSpirit(player))
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
                            future.complete(ISpiritManager.INSTANCE.getSpirit(player))
                        } else {
                            future.complete(0.0)
                        }
                    }
                }
            }
        }
    }
}