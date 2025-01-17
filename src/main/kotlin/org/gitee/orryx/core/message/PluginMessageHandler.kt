package org.gitee.orryx.core.message

import eos.moe.dragoncore.api.event.KeyPressEvent
import io.netty.buffer.Unpooled
import net.minecraft.server.v1_12_R1.PacketDataSerializer
import net.minecraft.server.v1_12_R1.PacketPlayOutCustomPayload
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.messaging.PluginMessageListener
import org.gitee.orryx.compat.dragoncore.DragonCoreCustomPacketSender
import org.gitee.orryx.core.profile.IPlayerKeySetting
import org.gitee.orryx.utils.DragonCoreEnabled
import org.gitee.orryx.utils.gson
import org.gitee.orryx.utils.toLocation
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.warning
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.PacketSender
import taboolib.platform.BukkitPlugin
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture

object PluginMessageHandler {

    private const val CHANNEL_NAME = "OrryxMod:main"

    private val playerFutureMap by lazy { mutableMapOf<UUID, CompletableFuture<AimInfo>>() }

    private val enable by lazy { MinecraftVersion.versionId == 11202 }

    @Awake(LifeCycle.ENABLE)
    private fun enable() {
        Bukkit.getMessenger().registerIncomingPluginChannel(BukkitPlugin.getInstance(), CHANNEL_NAME, Listener())
    }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        sendConfirm(e.player, true)
    }

    @Ghost
    @SubscribeEvent(priority = EventPriority.LOWEST)
    private fun keypress(e: KeyPressEvent) {
        if (!playerFutureMap.containsKey(e.player.uniqueId)) return
        if (IPlayerKeySetting.INSTANCE.aimConfirmKey(e.player) == e.key) {
            sendConfirm(e.player, false)
            e.isCancelled = true
        }
        if (IPlayerKeySetting.INSTANCE.aimCancelKey(e.player) == e.key) {
            sendConfirm(e.player, true)
            e.isCancelled = true
        }
    }

    @SubscribeEvent(ignoreCancelled = false)
    private fun interact(e: PlayerInteractEvent) {
        if (DragonCoreEnabled) return
        if (!playerFutureMap.containsKey(e.player.uniqueId)) return
        if (e.action == Action.LEFT_CLICK_AIR || e.action == Action.LEFT_CLICK_BLOCK) {
            sendConfirm(e.player, false)
            e.isCancelled = true
        }
        if (e.action == Action.RIGHT_CLICK_AIR || e.action == Action.RIGHT_CLICK_BLOCK) {
            sendConfirm(e.player, true)
            e.isCancelled = true
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private fun join(e: PlayerJoinEvent) {
        try {
            DragonCoreCustomPacketSender.sendKeyRegister(e.player)
        } catch (_: Throwable) {
        }
    }

    class Listener: PluginMessageListener {
        override fun onPluginMessageReceived(channel: String, player: Player, bytes: ByteArray) {
            val packet: AimBackPacket = try {
                gson.fromJson(String(bytes, StandardCharsets.UTF_8), AimBackPacket::class.java)
            } catch (e: Exception) {
                warning("玩家${player.name} 发送了不知名的包")
                return
            }
            val location = "${player.world.name},${packet.location}".toLocation()
            callAim(player, AimInfo(player, location, packet.skill, System.currentTimeMillis()))
        }
    }

    fun callAim(player: Player, aimInfo: AimInfo) {
        playerFutureMap.remove(player.uniqueId)?.apply {
            complete(aimInfo)
        }
    }

    fun sendAimAsk(player: Player, skill: String, scale: Double, range: Double, callback: (AimInfo) -> Unit) {
        if (!enable) {
            warning("指定性技能仅在1.12.2下可使用")
            return
        }
        val buffer = Unpooled.wrappedBuffer(
            ("call@" + gson.toJson(
                AimPacket(
                    skill,
                    enable = true,
                    max = range,
                    scale = scale
                )
            )).toByteArray(StandardCharsets.UTF_8)
        )
        val future = CompletableFuture<AimInfo>()
        playerFutureMap[player.uniqueId] = future
        PacketSender.sendPacket(
            player,
            PacketPlayOutCustomPayload("omega:main", PacketDataSerializer(buffer))
        )
        future.thenApply {
            callback(it)
        }
    }

    private fun sendConfirm(player: Player, cancel: Boolean) {
        if (!enable) {
            warning("指定性技能仅在1.12.2下可使用")
            return
        }
        if (cancel) playerFutureMap.remove(player.uniqueId)
        val buffer = Unpooled.wrappedBuffer(
            ("confirm@" + gson.toJson(
                AimConfirmPacket(cancel)
            )).toByteArray(StandardCharsets.UTF_8)
        )
        PacketSender.sendPacket(
            player,
            PacketPlayOutCustomPayload("omega:main", PacketDataSerializer(buffer))
        )
    }

    internal fun sendGhost(player: Player, timeout: Long) {
        if (!enable) {
            warning("鬼影仅在1.12.2下可使用")
            return
        }
        val buffer = Unpooled.wrappedBuffer(
            ("ghostTimeout@" + gson.toJson(
                GhostPacket(timeout * 50)
            )).toByteArray(StandardCharsets.UTF_8)
        )
        PacketSender.sendPacket(
            player,
            PacketPlayOutCustomPayload("omega:main", PacketDataSerializer(buffer))
        )
    }

    class AimInfo(val player: Player, val location: Location, val skill: String?, val time: Long)

    class AimPacket(
        val skill: String,
        val module: String = "default",
        val enable: Boolean,
        val max: Double,
        val scale: Double
    ) {
        override fun toString(): String {
            return "AimPacket(skill=$skill, module=$module, enable=$enable, max=$max, scale=$scale)"
        }
    }

    class AimConfirmPacket(
        val cancel: Boolean
    ) {
        override fun toString(): String {
            return "AimConfirmPacket(cancel=$cancel)"
        }
    }

    class AimBackPacket(
        val skill: String?,
        val location: String
    ) {
        override fun toString(): String {
            return "AimBackPacket(skill=$skill, location=$location)"
        }
    }

    class GhostPacket(
        val timeout: Long
    ) {
        override fun toString(): String {
            return "GhostPacket(timeout=$timeout)"
        }
    }

}
