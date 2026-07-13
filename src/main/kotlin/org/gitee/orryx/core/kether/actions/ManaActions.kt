package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.module.mana.ManaResult
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

object ManaActions {

    @KetherParser(["mana"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionMana() = scriptParser(
        Action.new("Mana法力", "检测法力", "mana", true)
            .description("检测是否有足够法力值")
            .addEntry("has", type = Type.SYMBOL, head = "has")
            .addEntry("检测法力值", type = Type.DOUBLE)
            .addContainerEntry("检测的目标", true, "@self")
            .result("是否有足够法力值", Type.BOOLEAN),
        Action.new("Mana法力", "给予法力", "mana", true)
            .description("玩家获得法力值")
            .addEntry("give", type = Type.SYMBOL, head = "give")
            .addEntry("法力值", type = Type.DOUBLE)
            .addContainerEntry("给予法力的目标", true, "@self"),
        Action.new("Mana法力", "减少法力", "mana", true)
            .description("获得法力值")
            .addEntry("take", type = Type.SYMBOL, head = "take")
            .addEntry("法力值", type = Type.DOUBLE)
            .addContainerEntry("减少法力的目标", true, "@self"),
        Action.new("Mana法力", "获取最大法力值", "mana", true)
            .description("获取玩家拥有的最大法力值")
            .addEntry("max", optional = true, type = Type.SYMBOL, head = "max")
            .addContainerEntry("获取的目标", true, "@self")
            .result("法力值", Type.DOUBLE),
        Action.new("Mana法力", "获取法力值", "mana", true)
            .description("获取玩家拥有的法力值")
            .addEntry("now", optional = true, type = Type.SYMBOL, head = "now")
            .addContainerEntry("获取的目标", true, "@self")
            .result("法力值", Type.DOUBLE)
    ) {
        it.switch {
            case("has") {
                val mana = it.nextParsedAction()
                val container = it.nextTheyContainerOrNull()
                actionFuture { future ->
                    run(mana).double { it }.thenCompose { amount ->
                        containerOrSelf(container) { targets ->
                            targets.firstInstanceOrNull<PlayerTarget>()?.getSource()
                        }.thenCompose { player ->
                            player?.let { IManaManager.INSTANCE.haveMana(it, amount) }
                                ?: CompletableFuture.completedFuture(false)
                        }
                    }.completeInto(future)
                }
            }
            case("give") {
                val mana = it.nextParsedAction()
                val they = it.nextTheyContainerOrNull()
                actionFuture { future ->
                    run(mana).double { it }.thenCompose { amount ->
                        containerOrSelf(they) { container ->
                            container.mapInstance<PlayerTarget, CompletableFuture<ManaResult>> { target ->
                                IManaManager.INSTANCE.giveMana(target.getSource(), amount)
                            }
                        }.thenCompose { operations ->
                            CompletableFuture.allOf(*operations.toTypedArray()).thenApply {
                                operations.map { it.getNow(ManaResult.CANCELLED) }
                            }
                        }
                    }.completeInto(future)
                }
            }
            case("take") {
                val mana = it.nextParsedAction()
                val they = it.nextTheyContainerOrNull()
                actionFuture { future ->
                    run(mana).double { it }.thenCompose { amount ->
                        containerOrSelf(they) { container ->
                            container.mapInstance<PlayerTarget, CompletableFuture<ManaResult>> { target ->
                                IManaManager.INSTANCE.takeMana(target.getSource(), amount)
                            }
                        }.thenCompose { operations ->
                            CompletableFuture.allOf(*operations.toTypedArray()).thenApply {
                                operations.map { it.getNow(ManaResult.CANCELLED) }
                            }
                        }
                    }.completeInto(future)
                }
            }
            case("max") {
                val they = it.nextTheyContainerOrNull()
                actionFuture { future ->
                    containerOrSelf(they) { container ->
                        container.firstInstanceOrNull<PlayerTarget>()?.getSource()
                    }.thenCompose { player ->
                        player?.let { IManaManager.INSTANCE.getMaxMana(it) }
                            ?: CompletableFuture.completedFuture(0.0)
                    }.completeInto(future)
                }
            }
            case("now") {
                val they = it.nextTheyContainerOrNull()
                actionFuture { future ->
                    containerOrSelf(they) { container ->
                        container.firstInstanceOrNull<PlayerTarget>()?.getSource()
                    }.thenCompose { player ->
                        player?.let { IManaManager.INSTANCE.getMana(it) }
                            ?: CompletableFuture.completedFuture(0.0)
                    }.completeInto(future)
                }
            }
            other {
                val they = it.nextTheyContainerOrNull()
                actionFuture { future ->
                    containerOrSelf(they) { container ->
                        container.firstInstanceOrNull<PlayerTarget>()?.getSource()
                    }.thenCompose { player ->
                        player?.let { IManaManager.INSTANCE.getMana(it) }
                            ?: CompletableFuture.completedFuture(0.0)
                    }.completeInto(future)
                }
            }
        }
    }
}