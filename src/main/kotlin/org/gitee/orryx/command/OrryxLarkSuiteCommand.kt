package org.gitee.orryx.command

import org.gitee.orryx.module.wiki.LarkSuite
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommandExec
import taboolib.expansion.createHelper

@CommandHeader("LarkSuite", ["ls"], "Orryx技能插件飞书文档指令", permission = "Orryx.Command.LarkSuite", permissionMessage = "你没有权限使用此指令")
object OrryxLarkSuiteCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val create = subCommandExec<ProxyCommandSender> {
        LarkSuite.createDocument()
    }
}