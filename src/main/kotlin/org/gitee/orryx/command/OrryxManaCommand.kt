package org.gitee.orryx.command

import org.gitee.orryx.core.mana.IManaManager
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
                    debug("${player.name}指令mana give结果${IManaManager.INSTANCE.giveMana(player, ctx["mana"].cdouble)}")
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
                    debug("${player.name}指令mana take结果${IManaManager.INSTANCE.takeMana(player, ctx["mana"].cdouble)}")
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
                    debug("${player.name}指令mana set结果${IManaManager.INSTANCE.setMana(player, ctx["mana"].cdouble)}")
                }
            }
        }
    }

    @CommandBody
    val regin = subCommand {
        player {
            exec<ProxyCommandSender> {
                val player = ctx.bukkitPlayer() ?: return@exec
                debug("${player.name}指令regin恢复法力${IManaManager.INSTANCE.reginMana(player)}")
            }
        }
    }

    @CommandBody
    val heal = subCommand {
        player {
            exec<ProxyCommandSender> {
                val player = ctx.bukkitPlayer() ?: return@exec
                debug("${player.name}指令heal恢复法力${IManaManager.INSTANCE.healMana(player)}")
            }
        }
    }

}