package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.*
import taboolib.platform.compat.depositBalance
import taboolib.platform.compat.getBalance
import taboolib.platform.compat.withdrawBalance

object MoneyActions {

    @KetherParser(["money"], namespace = ORRYX_NAMESPACE)
    private fun actionMoney() = scriptParser(
        Action.new("Money财富", "检测财富", "money")
            .description("检测是否有足够财富值")
            .addEntry("has", type = Type.SYMBOL, head = "has")
            .addEntry("检测财富值", type = Type.DOUBLE)
            .addContainerEntry("检测的目标们", true, "@self")
            .result("是否有足够财富值", Type.BOOLEAN),
        Action.new("Money财富", "给予财富", "money")
            .description("玩家获得财富值")
            .addEntry("add/deposit", type = Type.SYMBOL, head = "add/deposit")
            .addEntry("财富值", type = Type.DOUBLE)
            .addContainerEntry("给予财富的目标", true, "@self"),
        Action.new("Money财富", "减少财富", "money")
            .description("获得财富值")
            .addEntry("take/withdraw", type = Type.SYMBOL, head = "take/withdraw")
            .addEntry("财富值", type = Type.DOUBLE)
            .addContainerEntry("减少财富的目标", true, "@self"),
        Action.new("Money财富", "获取财富值", "money")
            .description("获取玩家拥有的财富值")
            .addEntry("get", optional = true, type = Type.SYMBOL, head = "get/look")
            .addContainerEntry("获取的目标", true, "@self")
            .result("财富值", Type.DOUBLE)
    ) {
        it.switch {
            case("has") {
                val money = it.nextParsedAction()
                val container = it.nextTheyContainerOrNull()
                actionFuture { complete ->
                    run(money).double { money ->
                        containerOrSelf(container) { container ->
                            complete.complete(
                                container.all<PlayerTarget> { target ->
                                    target.getSource().getBalance() >= money
                                }
                            )
                        }
                    }
                }
            }
            case("add", "deposit") {
                val money = it.nextParsedAction()
                val they = it.nextTheyContainerOrNull()
                actionNow {
                    run(money).double { money ->
                        containerOrSelf(they) { container ->
                            container.forEachInstance<PlayerTarget> { target ->
                                target.getSource().depositBalance(money)
                            }
                        }
                    }
                }
            }
            case("take", "withdraw") {
                val money = it.nextParsedAction()
                val they = it.nextTheyContainerOrNull()
                actionNow {
                    run(money).double { money ->
                        containerOrSelf(they) { container ->
                            container.forEachInstance<PlayerTarget> { target ->
                                target.getSource().withdrawBalance(money)
                            }
                        }
                    }
                }
            }
            case("get", "look") {
                val they = it.nextTheyContainerOrNull()
                actionFuture {
                    containerOrSelf(they) { container ->
                        it.complete(container.firstInstanceOrNull<PlayerTarget>()?.getSource()?.getBalance() ?: 0.0)
                    }
                }
            }
            other {
                val they = it.nextTheyContainerOrNull()
                actionFuture {
                    containerOrSelf(they) { container ->
                        it.complete(container.firstInstanceOrNull<PlayerTarget>()?.getSource()?.getBalance() ?: 0.0)
                    }
                }
            }
        }
    }
}