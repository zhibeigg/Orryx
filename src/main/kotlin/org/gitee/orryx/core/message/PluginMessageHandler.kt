package org.gitee.orryx.core.message

import com.germ.germplugin.api.GermPacketAPI
import com.germ.germplugin.api.KeyType
import com.germ.germplugin.api.event.GermKeyDownEvent
import com.google.common.io.ByteArrayDataInput
import com.google.common.io.ByteArrayDataOutput
import com.google.common.io.ByteStreams
import eos.moe.dragoncore.api.event.KeyPressEvent
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
import org.gitee.orryx.utils.DragonCorePlugin
import org.gitee.orryx.utils.GermPluginPlugin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import taboolib.common.util.unsafeLazy
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.BukkitPlugin
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object PluginMessageHandler {

    private const val CHANNEL_NAME = "orryxmod:main"
    private val pendingRequests = ConcurrentHashMap<UUID, Pair<Double, CompletableFuture<AimInfo>>>()

    private val isLegacyVersion by unsafeLazy { MinecraftVersion.versionId == 11202 }

    // 协议类型定义
    private sealed class PacketType(val header: Int) {
        data object AimRequest : PacketType(1)
        data object AimConfirm : PacketType(2)
        data object Ghost : PacketType(3)
        data object AimResponse : PacketType(4)
        data object Flicker : PacketType(5)
        data object PressAimRequest : PacketType(6)
        data object MouseRequest : PacketType(7)
    }

    @Awake(LifeCycle.ENABLE)
    private fun registerChannels() {
        with(Bukkit.getMessenger()) {
            registerIncomingPluginChannel(BukkitPlugin.getInstance(), CHANNEL_NAME, MessageReceiver())
            registerOutgoingPluginChannel(BukkitPlugin.getInstance(), CHANNEL_NAME)
        }
    }

    /* 事件处理 */
    @SubscribeEvent
    private fun onPlayerQuit(e: PlayerQuitEvent) {
        cleanupRequest(e.player)
    }

    @Ghost
    @SubscribeEvent(priority = EventPriority.LOWEST)
    private fun onKeyPress(e: KeyPressEvent) {
        if (e.isCancelled) return
        when (e.key.uppercase()) {
            IPlayerKeySetting.INSTANCE.aimConfirmKey(e.player) -> handleConfirmation(e.player, true)
            IPlayerKeySetting.INSTANCE.aimCancelKey(e.player) -> handleConfirmation(e.player, false)
            else -> return
        }.also { e.isCancelled = true }
    }

    @Ghost
    @SubscribeEvent(priority = EventPriority.LOWEST)
    private fun onKeyPress(e: GermKeyDownEvent) {
        if (e.isCancelled) return
        when (e.keyType.simpleKey.uppercase()) {
            IPlayerKeySetting.INSTANCE.aimConfirmKey(e.player) -> handleConfirmation(e.player, true)
            IPlayerKeySetting.INSTANCE.aimCancelKey(e.player) -> handleConfirmation(e.player, false)
            else -> return
        }.also { e.isCancelled = true }
    }

    @SubscribeEvent
    private fun onPlayerInteract(e: PlayerInteractEvent) {
        if (DragonCorePlugin.isEnabled || GermPluginPlugin.isEnabled) return
        when (e.action) {
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> handleConfirmation(e.player, true)
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> handleConfirmation(e.player, false)
            else -> return
        }.also { e.isCancelled = true }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private fun onPlayerJoin(e: PlayerJoinEvent) {
        if (GermPluginPlugin.isEnabled) {
            GermPacketAPI.sendKeyRegister(e.player, KeyType.valueOf("KEY_${IPlayerKeySetting.INSTANCE.aimConfirmKey(e.player)}").keyId)
            GermPacketAPI.sendKeyRegister(e.player, KeyType.valueOf("KEY_${IPlayerKeySetting.INSTANCE.aimCancelKey(e.player)}").keyId)
        }
        if (DragonCorePlugin.isEnabled) {
            try {
                DragonCoreCustomPacketSender.sendKeyRegister(e.player)
            } catch (ex: Throwable) {
                warning("DragonCore按键注册失败: ${ex.message}")
            }
        }
    }

    /* 公开API */
    /**
     * 发起瞄准请求
     * @param player 目标玩家
     * @param skillId 技能唯一标识
     * @param size 指示图大小
     * @param radius 瞄准半径（方块）
     * @param callback 结果回调（在主线程执行）
     */
    fun requestAiming(
        player: Player,
        skillId: String,
        picture: String,
        size: Double,
        radius: Double,
        callback: (Result<AimInfo>) -> Unit
    ) {
        if (!isLegacyVersion) {
            callback(Result.failure(UnsupportedVersionException()))
            return
        }

        CompletableFuture<AimInfo>().apply {
            pendingRequests[player.uniqueId] = radius to this
            whenComplete { result, ex ->
                submit { // 切换到主线程执行回调
                    callback(
                        if (ex == null) {
                            Result.success(result)
                        } else {
                            Result.failure(ex)
                        }
                    )
                }
            }
        }

        sendDataPacket(player, PacketType.AimRequest) {
            writeUTF(skillId)
            writeUTF(picture)
            writeDouble(size)
            writeDouble(radius)
        }
    }

    /**
     * 发起蓄力瞄准请求
     * @param player 目标玩家
     * @param skillId 技能唯一标识
     * @param picture 使用的图片组
     * @param min 指示图初始大小
     * @param max 指示图最大大小
     * @param radius 瞄准半径（方块）
     * @param maxTick 最大Tick
     * @param callback 结果回调（在主线程执行）
     */
    fun requestAiming(
        player: Player,
        skillId: String,
        picture: String,
        min: Double,
        max: Double,
        radius: Double,
        maxTick: Long,
        callback: (Result<AimInfo>) -> Unit
    ) {
        if (!isLegacyVersion) {
            callback(Result.failure(UnsupportedVersionException()))
            return
        }

        CompletableFuture<AimInfo>().apply {
            pendingRequests[player.uniqueId] = radius to this
            whenComplete { result, ex ->
                submit { // 切换到主线程执行回调
                    callback(
                        if (ex == null) {
                            Result.success(result)
                        } else {
                            Result.failure(ex)
                        }
                    )
                }
            }
        }

        sendDataPacket(player, PacketType.PressAimRequest) {
            writeUTF(skillId)
            writeUTF(picture)
            writeDouble(min)
            writeDouble(max)
            writeDouble(radius)
            writeLong(maxTick)
        }
    }

    /**
     * 应用鬼影效果，移动中产生跟随身体的魂影
     * @param viewer 可视玩家
     * @param player 效果玩家
     * @param duration 持续时间（毫秒）
     * @param density 密度
     * @param gap 间隔
     */
    fun applyGhostEffect(viewer: Player, player: Player, duration: Long, density: Int, gap: Int) {
        sendDataPacket(viewer, PacketType.Ghost) {
            writeUTF(player.uniqueId.toString())
            writeLong(duration)
            writeInt(density)
            writeInt(gap)
        }
    }

    /**
     * 应用闪影效果，原地留下一道虚影
     * @param viewer 可视玩家
     * @param player 效果玩家
     * @param duration 持续时间（毫秒）
     * @param alpha 透明度（0.0-1.0）
     */
    fun applyFlickerEffect(viewer: Player, player: Player, duration: Long, alpha: Float) {
        sendDataPacket(viewer, PacketType.Flicker) {
            writeUTF(player.uniqueId.toString())
            writeLong(duration)
            writeFloat(alpha)
        }
    }

    /**
     * 设置鼠标状态
     * @param player 玩家
     * @param show 是否呼出
     */
    fun applyMouseCursor(player: Player, show: Boolean) {
        if (GermPluginPlugin.isEnabled) {
            GermPacketAPI.setPlayerFocus(player, show)
        } else {
            sendDataPacket(player, PacketType.MouseRequest) {
                writeBoolean(show)
            }
        }
    }

    /* 内部实现 */
    private fun handleConfirmation(player: Player, isConfirmed: Boolean) {
        pendingRequests[player.uniqueId] ?: return
        sendDataPacket(player, PacketType.AimConfirm) {
            writeBoolean(isConfirmed)
        }
        if (!isConfirmed) cleanupRequest(player)
    }

    private fun cleanupRequest(player: Player) {
        pendingRequests.remove(player.uniqueId)?.apply {
            second.completeExceptionally(PlayerCancelledException())
        }
    }

    private inline fun sendDataPacket(
        player: Player,
        type: PacketType,
        block: ByteArrayDataOutput.() -> Unit
    ) {
        try {
            val output = ByteStreams.newDataOutput().apply {
                writeInt(type.header)
                block()
            }
            player.sendPluginMessage(
                BukkitPlugin.getInstance(),
                CHANNEL_NAME,
                output.toByteArray()
            )
        } catch (ex: Exception) {
            warning("给玩家 ${player.name} 发送数据包失败: ${ex.message}")
        }
    }

    /* 消息接收处理器 */
    private class MessageReceiver : PluginMessageListener {
        override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
            if (channel != CHANNEL_NAME) return

            val input = ByteStreams.newDataInput(message)
            try {
                when (val header = input.readInt()) {
                    PacketType.AimResponse.header -> handleAimResponse(player, input)
                    else -> warning("收到未知数据包类型: $header")
                }
            } catch (ex: Exception) {
                warning("处理来自 ${player.name} 的数据包时出错: ${ex.message}")
            }
        }

        private fun handleAimResponse(player: Player, input: ByteArrayDataInput) {
            try {
                val skillId = input.readUTF()
                val location = readLocation(player, input)

                if (location.distance(player.location) <= ((pendingRequests[player.uniqueId]?.first ?: 0.0) + 5.0)) {
                    pendingRequests.remove(player.uniqueId)?.second?.complete(AimInfo(player, location, skillId))
                } else {
                    pendingRequests.remove(player.uniqueId)
                    warning("玩家${player.name} 向服务器发送了作弊 超远释放${skillId}技能数据包")
                }
            } catch (ex: Exception) {
                warning("解析瞄准数据包失败: ${ex.message}")
            }
        }

        private fun readLocation(player: Player, input: ByteArrayDataInput): Location {
            return Location(
                player.world,
                input.readDouble(), // X
                input.readDouble(), // Y
                input.readDouble()  // Z
            )
        }
    }

    /* 数据类 */
    data class AimInfo(
        val player: Player,
        val location: Location,
        val skillId: String?,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        override fun toString(): String {
            return "AimInfo(player=$player, location=$location, skillId=$skillId, timestamp=$timestamp)"
        }
    }

    /* 异常体系 */
    class UnsupportedVersionException : IllegalStateException("此功能仅支持 1.12.2 版本")

    class PlayerCancelledException : RuntimeException("玩家取消操作")

}