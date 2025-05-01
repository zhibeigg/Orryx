package org.gitee.orryx.command

import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.debug
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.player
import taboolib.common.platform.command.restrictDouble
import taboolib.common.platform.command.subCommand
import taboolib.common5.cdouble

object OrryxManaCommand {

    @CommandBody
    val give = subCommand {
        player {
            dynamic("mana") {
                restrictDouble()
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    IManaManager.INSTANCE.giveMana(player, ctx["mana"].cdouble).thenApply {
                        sender.sendMessage("result: $it")
                        debug("${player.name}指令mana give结果${it}")
                    }
                }
            }
        }
    }

    @CommandBody
    val take = subCommand {
        player {
            dynamic("mana") {
                restrictDouble()
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    IManaManager.INSTANCE.takeMana(player, ctx["mana"].cdouble).thenApply {
                        sender.sendMessage("result: $it")
                        debug("${player.name}指令mana take结果${it}")
                    }
                }
            }
        }
    }

    @CommandBody
    val set = subCommand {
        player {
            dynamic("mana") {
                restrictDouble()
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    IManaManager.INSTANCE.setMana(player, ctx["mana"].cdouble).thenApply {
                        sender.sendMessage("result: $it")
                        debug("${player.name}指令mana set结果${it}")
                    }
                }
            }
        }
    }

    @CommandBody
    val regain = subCommand {
        player {
            exec<ProxyCommandSender> {
                val player = ctx.bukkitPlayer() ?: return@exec
                IManaManager.INSTANCE.regainMana(player).thenApply {
                    sender.sendMessage("玩家${player.name} 恢复了 $it 点法力")
                    debug("${player.name}指令regain恢复法力${it}")
                }
            }
        }
    }

    @CommandBody
    val heal = subCommand {
        player {
            exec<ProxyCommandSender> {
                val player = ctx.bukkitPlayer() ?: return@exec
                IManaManager.INSTANCE.healMana(player).thenApply {
                    sender.sendMessage("玩家${player.name} 恢复了 $it 点法力")
                    debug("${player.name}指令heal恢复法力${it}")
                }
            }
        }
    }
}