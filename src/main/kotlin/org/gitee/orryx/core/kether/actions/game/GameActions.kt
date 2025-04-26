package org.gitee.orryx.core.kether.actions.game

import org.bukkit.entity.Entity
import org.bukkit.potion.PotionEffect
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.library.xseries.XPotion
import taboolib.module.kether.*

object GameActions {

    @KetherParser(["sprint"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionSprint() = combinationParser(
        Action.new("Game原版游戏", "设置跑步状态", "sprint", true)
            .description("设置跑步状态")
            .addEntry("是否跑步", Type.BOOLEAN, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            bool(),
            theyContainer(true)
        ).apply(it) { isSprint, container ->
            future {
                ensureSync {
                    container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                        player.getSource().isSprinting = isSprint
                    }
                }
            }
        }
    }

    @KetherParser(["flyHeight"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionFlyHeight() = combinationParser(
        Action.new("Game原版游戏", "获得玩家离地面高度", "flyHeight", true)
            .description("获得玩家离地面高度")
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            theyContainer(true)
        ).apply(it) { container ->
            future {
                val player = container.orElse(self()).firstInstanceOrNull<PlayerTarget>() ?: return@future completedFuture(null)
                completedFuture(floor(player.getSource().location.clone(), 256.0).second)
            }
        }
    }

    @KetherParser(["specialTarget"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionSpecialTarget() = combinationParser(
        Action.new("Game原版游戏", "设置旁观者模式下的附着视角", "specialTarget", true)
            .description("设置旁观者模式下的附着视角")
            .addEntry("目标实体", Type.CONTAINER)
            .addContainerEntry("玩家", optional = true, default = "@self")
    ) {
        it.group(
            container(),
            theyContainer(false)
        ).apply(it) { target, container ->
            future {
                ensureSync {
                    container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                        target?.firstInstanceOrNull<ITargetEntity<Entity>>()?.getSource()
                            ?.let { entity -> player.getSource().spectatorTarget = entity }
                    }
                }
            }
        }
    }

    @KetherParser(["potion"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionPotion() = scriptParser(
        Action.new("Game原版游戏", "设置药水效果", "potion", true)
            .description("设置药水效果")
            .addEntry("设置标识符", Type.SYMBOL, false, head = "set")
            .addEntry("效果", Type.STRING)
            .addEntry("持续时间", Type.INT, false)
            .addEntry("等级", Type.INT, true, "1", "level")
            .addContainerEntry("玩家", optional = true, default = "@self"),
        Action.new("Game原版游戏", "删除药水效果", "potion", true)
            .description("删除药水效果")
            .addEntry("删除标识符", Type.SYMBOL, false, head = "remove/delete")
            .addEntry("效果", Type.STRING)
            .addContainerEntry("实体", optional = true, default = "@self"),
        Action.new("Game原版游戏", "清除所有药水效果", "potion", true)
            .description("清除所有药水效果")
            .addEntry("清除标识符", Type.SYMBOL, false, head = "clear")
            .addContainerEntry("实体", optional = true, default = "@self")
    ) {
        it.switch {
            case("set") {
                val effect = nextToken().uppercase()
                val duration = nextParsedAction()
                val level = nextHeadAction("level", 1)
                val they = nextTheyContainerOrNull()
                actionFuture { future ->
                    run(duration).int { duration ->
                        run(level).int { level ->
                            containerOrSelf(they) { container ->
                                ensureSync {
                                    val type = XPotion.matchXPotion(effect).orElse(XPotion.SPEED).potionEffectType
                                        ?: return@ensureSync future.complete(false)
                                    val potion = PotionEffect(type, duration, level, false, false)
                                    future.complete(
                                        container.mapNotNullInstance<ITargetEntity<Entity>, Boolean> { target ->
                                            target.entity.getBukkitLivingEntity()?.addPotionEffect(potion, true)
                                        }.all { b -> b }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            case("remove", "delete") {
                val effect = nextToken().uppercase()
                val they = nextTheyContainerOrNull()
                actionFuture { future ->
                    containerOrSelf(they) { container ->
                        ensureSync {
                            val type = XPotion.matchXPotion(effect).orElse(XPotion.SPEED).potionEffectType
                                ?: return@ensureSync future.complete(null)
                            container.forEachInstance<ITargetEntity<Entity>> { target ->
                                target.entity.getBukkitLivingEntity()?.removePotionEffect(type)
                            }
                            future.complete(null)
                        }
                    }
                }
            }
            case("clear") {
                val they = nextTheyContainerOrNull()
                actionFuture { future ->
                    containerOrSelf(they) { container ->
                        ensureSync {
                            container.forEachInstance<ITargetEntity<Entity>> { target ->
                                target.entity.getBukkitLivingEntity()?.apply {
                                    activePotionEffects.forEach { effect ->
                                        removePotionEffect(effect.type)
                                    }
                                }
                            }
                            future.complete(null)
                        }
                    }
                }
            }
        }
    }
}