package org.gitee.orryx.command

import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.subCommandExec
import taboolib.common.platform.function.info
import taboolib.platform.util.sendLang
import java.util.*
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

object OrryxTestCommand {

    private val unlimits = mutableListOf<UUID>()

    fun isUnlimited(player: Player): Boolean {
        return unlimits.contains(player.uniqueId)
    }

    @CommandBody
    val unlimit = subCommand {
        exec<Player> {
            if (unlimits.contains(sender.uniqueId)) {
                unlimits.remove(sender.uniqueId)
                sender.sendLang("out-unlimit-mode")
            } else {
                unlimits.add(sender.uniqueId)
                sender.sendLang("in-unlimit-mode")
            }
        }
    }

    @CommandBody
    val test = subCommandExec<ProxyCommandSender> {
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScript>()

        val host = BasicJvmScriptingHost()

        val scriptContent = """
            println("Hello from Kotlin Script!")
            val x = 10
            val y = 20
            x + y
        """.trimIndent()

        info(
            host.eval(
                scriptContent.toScriptSource(),
                compilationConfiguration,
                null
            )
        )
    }

    @KotlinScript
    abstract class SimpleScript
}