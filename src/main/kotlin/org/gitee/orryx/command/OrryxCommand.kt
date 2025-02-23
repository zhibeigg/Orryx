package org.gitee.orryx.command

import org.bukkit.entity.Player
import org.gitee.orryx.core.reload.ReloadAPI
import org.gitee.orryx.utils.joml
import org.gitee.orryx.utils.raytrace.FluidHandling
import org.gitee.orryx.utils.raytrace.SpigotWorld
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyParticle
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommandExec
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.util.Location
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
    val test = subCommandExec<Player> {
        val pos = SpigotWorld(sender.world).rayTraceBlocks(
            sender.eyeLocation.toVector().joml(),
            sender.location.direction.joml().normalize(10.0),
            1.0,
            FluidHandling.SOURCE_ONLY,
            checkAxisAlignedBB = true,
            returnClosestPos = true
        )?.hitPosition ?: return@subCommandExec
        ProxyParticle.DUST.sendTo(adaptPlayer(sender), Location(sender.world.name, pos.x, pos.y, pos.z))
    }

}