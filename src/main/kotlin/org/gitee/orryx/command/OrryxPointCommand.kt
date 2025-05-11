package org.gitee.orryx.command

import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.orryxProfile
import org.gitee.orryx.utils.orryxProfileTo
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.int
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common5.cint

object OrryxPointCommand {

    @CommandBody
    val give = subCommand {
        player {
            int("point") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    player.orryxProfileTo {
                        it.givePoint(ctx["point"].cint)
                        sender.sendMessage("玩家${player.name} 技能点添加成功")
                    }
                }
            }
        }
    }

    @CommandBody
    val take = subCommand {
        player {
            int("point") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    player.orryxProfileTo {
                        it.takePoint(ctx["point"].cint)
                        sender.sendMessage("玩家${player.name} 技能点减少成功")
                    }
                }
            }
        }
    }

    @CommandBody
    val set = subCommand {
        player {
            int("point") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    player.orryxProfileTo {
                        it.setPoint(ctx["point"].cint)
                        sender.sendMessage("玩家${player.name} 技能点设置成功")
                    }
                }
            }
        }
    }
}