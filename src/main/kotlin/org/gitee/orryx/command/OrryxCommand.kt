package org.gitee.orryx.command

import org.bukkit.entity.Player
import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.kether.actions.effect.EffectBuilder
import org.gitee.orryx.core.kether.actions.effect.EffectSpawner
import org.gitee.orryx.core.kether.actions.effect.EffectType
import org.gitee.orryx.core.reload.ReloadAPI
import org.gitee.orryx.utils.toTarget
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyParticle
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
    val test = subCommandExec<Player> {
        val builder = EffectBuilder().apply {
            type = EffectType.ARC
            particle = ProxyParticle.DUST
            startAngle = 0.0
            angle = 360.0
            radius = 2.0
            count = 5
            speed = 0.1
            step = 0.2
            period = 1
        }
        EffectSpawner(builder, origins = Container(mutableSetOf(sender.toTarget())), viewers = Container(mutableSetOf(sender.toTarget()))).start()
    }

}