package org.gitee.orryx.command

import org.gitee.orryx.module.wiki.LarkSuite
import org.gitee.orryx.module.wiki.MarkdownGenerator
import org.gitee.orryx.module.wiki.ActionsSchemaGenerator
import org.gitee.orryx.utils.consoleMessage
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommandExec
import taboolib.common.platform.function.getDataFolder
import taboolib.expansion.createHelper
import java.io.File

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

    @CommandBody
    val markdown = subCommandExec<ProxyCommandSender> {
        val outputFile = File(getDataFolder(), "wiki.md")
        MarkdownGenerator.generate(outputFile)
        consoleMessage("&e┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        consoleMessage("&e┣&7Markdown文档已生成 &a√")
        consoleMessage("&e┣&7路径: &f${outputFile.absolutePath}")
        consoleMessage("&e┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    @CommandBody
    val schema = subCommandExec<ProxyCommandSender> {
        val outputFile = File(getDataFolder(), "actions-schema.json")
        ActionsSchemaGenerator.generate(outputFile)
        consoleMessage("&e┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        consoleMessage("&e┣&7Actions Schema 已生成 &a√")
        consoleMessage("&e┣&7路径: &f${outputFile.absolutePath}")
        consoleMessage("&e┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}