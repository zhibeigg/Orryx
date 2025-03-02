package org.gitee.orryx.command

import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.reload.ReloadAPI
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommandExec
import taboolib.expansion.createHelper

@CommandHeader("Orryx", ["or"], "Orryx技能插件主指令", permission = "Orryx.Command.Main", permissionMessage = "你没有权限使用此指令")
object OrryxCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val reload = subCommandExec<ProxyCommandSender> {
        ReloadAPI.reload()
        sender.sendMessage("Orryx重载成功")
    }

    @CommandBody
    val job = OrryxJobCommand

    @CommandBody
    val mana = OrryxManaCommand

    @CommandBody
    val point = OrryxPointCommand

    @CommandBody
    val skill = OrryxSkillCommand

    @CommandBody
    val ui = OrryxUICommand

    @CommandBody
    val script = OrryxScriptCommand

    @CommandBody
    val test = subCommandExec<Player> {
        ScriptManager.runningSkillScriptsMap[sender.uniqueId]?.terminate("test")
    }

    @CommandBody
    val shutdown = subCommandExec<ConsoleCommandSender> {
        GameManager.shutdownServer()
    }

}