package org.gitee.orryx.core.message

import com.eatthepath.uuid.FastUUID
import com.germ.germplugin.api.GermPacketAPI
import com.germ.germplugin.api.event.GermKeyDownEvent
import com.google.common.io.ByteArrayDataInput
import com.google.common.io.ByteArrayDataOutput
import com.google.common.io.ByteStreams
import org.gitee.orryx.api.collider.ICollider
import eos.moe.dragoncore.api.event.KeyPressEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.messaging.PluginMessageListener
import org.gitee.orryx.core.message.bloom.BloomConfig
import org.gitee.orryx.core.message.collider.ColliderRenderColor
import org.gitee.orryx.core.message.collider.ColliderWireCodec
import org.gitee.orryx.core.message.collider.ColliderWireSnapshot
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.*
import priv.seventeen.artist.arcartx.event.client.ClientKeyPressEvent
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
import java.util.concurrent.atomic.AtomicLong

object PluginMessageHandler {

    private const val CHANNEL_NAME = "orryxmod:main"
    internal val pendingRequests = ConcurrentHashMap<UUID, PendingAimRequest>()
    private val requestSequence = AtomicLong()
    private const val AIM_TIMEOUT_SECONDS = 30L
    private const val AIM_RESPONSE_GRACE_TICKS = 40L
    private const val MAX_AIM_PRESS_TICKS = 6_000L
    private const val MAX_PICTURE_LENGTH = 1024
    private const val MAX_PLUGIN_MESSAGE_BYTES = 32_766

    private val isLegacyVersion by unsafeLazy { MinecraftVersion.versionId == 11202 }

    // 协议类型定义
    internal sealed class PacketType(val header: Int) {
        data object AimRequest : PacketType(1)
        data object AimConfirm : PacketType(2)
        data object Ghost : PacketType(3)
        data object AimResponse : PacketType(4)
        data object Flicker : PacketType(5)
        data object PressAimRequest : PacketType(6)
        data object MouseRequest : PacketType(7)
        data object EntityShow : PacketType(8)
        data object EntityShowRemove : PacketType(9)
        data object PlayerNavigation : PacketType(10)
        data object PlayerNavigationStop : PacketType(11)
        data object SquareShockwave : PacketType(12)
        data object CircleShockwave : PacketType(13)
        data object SectorShockwave : PacketType(14)
        data object BloomConfigSync : PacketType(15)
        data object BloomConfigUpdate : PacketType(16)
        data object BloomConfigRemove : PacketType(17)
        data object ColliderShow : PacketType(18)
        data object ColliderUpdate : PacketType(19)
        data object ColliderRemove : PacketType(20)
    }

    @Awake(LifeCycle.ENABLE)
    private fun registerChannels() {
        with(Bukkit.getMessenger()) {
            registerIncomingPluginChannel(BukkitPlugin.getInstance(), CHANNEL_NAME, MessageReceiver())
            registerOutgoingPluginChannel(BukkitPlugin.getInstance(), CHANNEL_NAME)
        }
    }

    @Reload(1)
    private fun clean() {
        pendingRequests.values.forEach { it.fail(PlayerCancelledException()) }
        pendingRequests.clear()
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
        e.player.keySetting {
            when (e.key.uppercase()) {
                it.aimConfirmKey -> handleConfirmation(e.player, true)
                it.aimCancelKey -> handleConfirmation(e.player, false)
                else -> return@keySetting
            }.also { check ->
                if (check) { e.isCancelled = true }
            }
        }
    }

    @Ghost
    @SubscribeEvent(priority = EventPriority.LOWEST)
    private fun onKeyPress(e: GermKeyDownEvent) {
        if (e.isCancelled) return
        e.player.keySetting {
            val key = when (e.keyType.simpleKey) {
                "MLEFT" -> MOUSE_LEFT
                "MRIGHT" -> MOUSE_RIGHT
                else -> it
            }
            when (key) {
                it.aimConfirmKey -> handleConfirmation(e.player, true)
                it.aimCancelKey -> handleConfirmation(e.player, false)
                else -> return@keySetting
            }.also { check ->
                if (check) { e.isCancelled = true }
            }
        }
    }

    @Ghost
    @SubscribeEvent(priority = EventPriority.LOWEST)
    private fun onKeyPress(e: ClientKeyPressEvent) {
        e.player.keySetting {
            when (e.keyName.uppercase()) {
                it.aimConfirmKey -> handleConfirmation(e.player, true)
                it.aimCancelKey -> handleConfirmation(e.player, false)
                else -> return@keySetting
            }
        }
    }

    @SubscribeEvent
    private fun onPlayerInteract(e: PlayerInteractEvent) {
        if (DragonCorePlugin.isEnabled || GermPluginPlugin.isEnabled || ArcartXPlugin.isEnabled) return
        when (e.action) {
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> handleConfirmation(e.player, true)
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> handleConfirmation(e.player, false)
            else -> return
        }.also { check ->
            if (check) {
                e.isCancelled = true
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
        validateAimRequest(skillId, picture, radius, size)?.let {
            callback(Result.failure(it))
            return
        }

        val request = registerPendingRequest(
            player = player,
            skillId = skillId,
            maxDistance = radius,
            timeoutTicks = AIM_TIMEOUT_SECONDS * 20,
            callback = callback,
        )
        val sent = sendDataPacket(player, PacketType.AimRequest) {
            writeUTF(request.wireSkillId)
            writeUTF(picture)
            writeDouble(size)
            writeDouble(radius)
        }
        if (!sent) {
            failPendingRequest(player.uniqueId, request, AimPacketException("瞄准请求发送失败"))
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
        validateAimRequest(skillId, picture, radius, min, max)?.let {
            callback(Result.failure(it))
            return
        }
        if (max < min || maxTick <= 0L || maxTick > MAX_AIM_PRESS_TICKS) {
            callback(Result.failure(IllegalArgumentException("蓄力瞄准参数无效，maxTick 必须在 1..$MAX_AIM_PRESS_TICKS 内")))
            return
        }

        val request = registerPendingRequest(
            player = player,
            skillId = skillId,
            maxDistance = radius,
            timeoutTicks = maxTick + AIM_RESPONSE_GRACE_TICKS,
            callback = callback,
        )
        val sent = sendDataPacket(player, PacketType.PressAimRequest) {
            writeUTF(request.wireSkillId)
            writeUTF(picture)
            writeDouble(min)
            writeDouble(max)
            writeDouble(radius)
            writeLong(maxTick)
        }
        if (!sent) {
            failPendingRequest(player.uniqueId, request, AimPacketException("蓄力瞄准请求发送失败"))
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
            writeUTF(FastUUID.toString(player.uniqueId))
            writeLong(duration)
            writeInt(density)
            writeInt(gap)
        }
    }

    /**
     * 应用闪影效果，原地留下一道虚影
     * @param viewer 可视玩家
     * @param player 效果玩家
     * @param timeout 持续时间（毫秒）
     * @param alpha 透明度（0.0-1.0）
     * @param duration 透明度淡化时间(-1为不淡化)
     * @param scale 缩放
     */
    fun applyFlickerEffect(viewer: Player, player: Player, timeout: Long, alpha: Float, duration: Long, scale: Float) {
        sendDataPacket(viewer, PacketType.Flicker) {
            writeUTF(FastUUID.toString(player.uniqueId))
            writeLong(timeout)
            writeFloat(alpha)
            writeLong(duration)
            writeFloat(scale)
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

    /**
     * 应用投影效果，在指定地点投影一个玩家虚影
     * @param viewer 可视玩家
     * @param entity 效果实体
     * @param group 组
     * @param location 位置
     * @param timeout 持续时间（毫秒）
     * @param rotateX X轴旋转
     * @param rotateY Y轴旋转
     * @param rotateZ Z轴旋转
     * @param scale 缩放
     * @param alpha 透明度 (0.0-1.0)
     * @param fadeOut 是否启用渐隐效果
     */
    fun applyEntityShowEffect(viewer: Player, entity: UUID, group: String, location: Location, timeout: Long, rotateX: Float, rotateY: Float, rotateZ: Float, scale: Float, alpha: Float, fadeOut: Boolean) {
        if (viewer.world != location.world) return
        sendDataPacket(viewer, PacketType.EntityShow) {
            writeUTF(FastUUID.toString(entity))
            writeUTF(group)
            writeDouble(location.x)
            writeDouble(location.y)
            writeDouble(location.z)
            writeLong(timeout)
            writeFloat(rotateX)
            writeFloat(rotateY)
            writeFloat(rotateZ)
            writeFloat(scale)
            writeFloat(alpha)
            writeBoolean(fadeOut)
        }
    }

    /**
     * 删除投影效果
     * @param entity 效果实体
     * @param group 组
     */
    fun removeEntityShowEffect(viewer: Player, entity: UUID, group: String) {
        sendDataPacket(viewer, PacketType.EntityShowRemove) {
            writeUTF(FastUUID.toString(entity))
            writeUTF(group)
        }
    }

    /**
     * 发起客户端寻路导航
     * @param player 玩家
     * @param x x
     * @param y y
     * @param z z
     * @param range 目标点范围半径
     */
    fun playerNavigation(player: Player, x: Int, y: Int, z: Int, range: Int) {
        sendDataPacket(player, PacketType.PlayerNavigation) {
            writeInt(x)
            writeInt(y)
            writeInt(z)
            writeInt(range)
        }
    }

    /**
     * 停止客户端寻路导航
     * @param player 玩家
     */
    fun stopPlayerNavigation(player: Player) {
        sendDataPacket(player, PacketType.PlayerNavigationStop)
    }

    /**
     * 发送圆形地震波效果
     * @param player 玩家
     * @param x x
     * @param y y
     * @param z z
     * @param r 半径
     */
    fun sendCircleShockwave(player: Player, x: Double, y: Double, z: Double, r: Double) {
        sendDataPacket(player, PacketType.CircleShockwave) {
            writeDouble(x)
            writeDouble(y)
            writeDouble(z)
            writeDouble(r)
        }
    }

    /**
     * 发送方形地震波效果
     * @param player 玩家
     * @param x x
     * @param y y
     * @param z z
     * @param width 宽度
     * @param length 长度
     * @param yaw 方向
     */
    fun sendSquareShockwave(player: Player, x: Double, y: Double, z: Double, width: Double, length: Double, yaw: Double) {
        sendDataPacket(player, PacketType.SquareShockwave) {
            writeDouble(x)
            writeDouble(y)
            writeDouble(z)
            writeDouble(length)
            writeDouble(width)
            writeDouble(yaw)
        }
    }

    /**
     * 发送扇形地震波效果
     * @param player 玩家
     * @param x x
     * @param y y
     * @param z z
     * @param r 半径
     * @param yaw 方向
     * @param angle 开合角度
     */
    fun sendSectorShockwave(player: Player, x: Double, y: Double, z: Double, r: Double, yaw: Double, angle: Double) {
        sendDataPacket(player, PacketType.SectorShockwave) {
            writeDouble(x)
            writeDouble(y)
            writeDouble(z)
            writeDouble(r)
            writeDouble(angle)
            writeDouble(yaw)
        }
    }

    /**
     * 同步所有 Bloom 配置到玩家
     * @param player 目标玩家
     * @param configs 配置映射
     */
    fun sendBloomConfigSync(player: Player, configs: Map<String, BloomConfig>) {
        sendDataPacket(player, PacketType.BloomConfigSync) {
            writeInt(configs.size)
            configs.forEach { (id, config) ->
                writeUTF(id)
                writeUTF(config.name)
                writeInt(config.r)
                writeInt(config.g)
                writeInt(config.b)
                writeInt(config.a)
                writeFloat(config.strength)
                writeFloat(config.radius)
                writeInt(config.priority)
            }
        }
    }

    /**
     * 更新单个 Bloom 配置
     * @param player 目标玩家
     * @param config 配置
     */
    fun sendBloomConfigUpdate(player: Player, config: BloomConfig) {
        sendDataPacket(player, PacketType.BloomConfigUpdate) {
            writeUTF(config.id)
            writeUTF(config.name)
            writeInt(config.r)
            writeInt(config.g)
            writeInt(config.b)
            writeInt(config.a)
            writeFloat(config.strength)
            writeFloat(config.radius)
            writeInt(config.priority)
        }
    }

    /**
     * 删除 Bloom 配置
     * @param player 目标玩家
     * @param id 配置ID
     */
    fun sendBloomConfigRemove(player: Player, id: String) {
        sendDataPacket(player, PacketType.BloomConfigRemove) {
            writeUTF(id)
        }
    }

    /**
     * 发送碰撞箱创建/显示数据包。
     *
     * 该低层入口只负责一次性发送；需要自动跟踪更新时使用 ColliderSyncManager。
     */
    fun sendColliderShow(viewer: Player, id: String, collider: ICollider<*>, r: Int, g: Int, b: Int, a: Int) {
        val color = ColliderRenderColor.clamped(r, g, b, a)
        val snapshot = runCatching { ColliderWireCodec.snapshot(id, collider, color) }
            .onFailure { warning("无法序列化碰撞箱 $id: ${it.message}") }
            .getOrNull()
            ?: return
        sendColliderShow(viewer, snapshot)
    }

    /** 发送碰撞箱更新数据包；复合体子节点默认使用白色。 */
    fun sendColliderUpdate(viewer: Player, id: String, collider: ICollider<*>) {
        val snapshot = runCatching { ColliderWireCodec.snapshot(id, collider, ColliderRenderColor.WHITE) }
            .onFailure { warning("无法序列化碰撞箱 $id: ${it.message}") }
            .getOrNull()
            ?: return
        sendColliderUpdate(viewer, snapshot)
    }

    /** 发送碰撞箱移除数据包。 */
    fun sendColliderRemove(viewer: Player, id: String) {
        sendColliderRemovePacket(viewer, id)
    }

    internal fun sendColliderShow(viewer: Player, snapshot: ColliderWireSnapshot): Boolean {
        return sendDataPacket(viewer, PacketType.ColliderShow) {
            ColliderWireCodec.writeShowPayload(this, snapshot)
        }
    }

    internal fun sendColliderUpdate(viewer: Player, snapshot: ColliderWireSnapshot): Boolean {
        return sendDataPacket(viewer, PacketType.ColliderUpdate) {
            ColliderWireCodec.writeUpdatePayload(this, snapshot)
        }
    }

    internal fun sendColliderRemovePacket(viewer: Player, id: String): Boolean {
        if (id.isBlank() || id.length > ColliderWireCodec.MAX_ID_LENGTH) return false
        return sendDataPacket(viewer, PacketType.ColliderRemove) {
            writeUTF(id)
        }
    }

    /* 内部实现 */
    private fun validateAimRequest(skillId: String, picture: String, radius: Double, vararg sizes: Double): Throwable? {
        if (skillId.isBlank() || skillId.length > AimRequestProtocol.MAX_SKILL_ID_LENGTH) {
            return IllegalArgumentException("技能标识无效")
        }
        if (picture.length > MAX_PICTURE_LENGTH) {
            return IllegalArgumentException("瞄准图片标识过长")
        }
        if (!radius.isFinite() || radius < 0.0 || sizes.any { !it.isFinite() || it < 0.0 }) {
            return IllegalArgumentException("瞄准范围参数必须是非负有限值")
        }
        val maxDistance = radius + (sizes.maxOrNull() ?: 0.0)
        if (!maxDistance.isFinite() || !(maxDistance * maxDistance).isFinite()) {
            return IllegalArgumentException("瞄准范围参数过大")
        }
        return null
    }

    private fun registerPendingRequest(
        player: Player,
        skillId: String,
        maxDistance: Double,
        timeoutTicks: Long,
        callback: (Result<AimInfo>) -> Unit,
    ): PendingAimRequest {
        val future = CompletableFuture<AimInfo>()
        val requestId = requestSequence.incrementAndGet()
        val request = PendingAimRequest(
            requestId = requestId,
            skillId = skillId,
            wireSkillId = AimRequestProtocol.createWireSkillId(skillId, requestId),
            maxDistance = maxDistance,
            future = future,
        )
        pendingRequests.put(player.uniqueId, request)?.fail(AimSupersededException())

        future.whenComplete { result, ex ->
            pendingRequests.remove(player.uniqueId, request)
            val outcome = if (ex == null) Result.success(result) else Result.failure(ex)
            try {
                submit { callback(outcome) }
            } catch (scheduleFailure: Throwable) {
                callback(Result.failure(scheduleFailure))
            }
        }
        try {
            submit(delay = timeoutTicks) {
                failPendingRequest(
                    player.uniqueId,
                    request,
                    java.util.concurrent.TimeoutException("瞄准请求超时"),
                )
            }
        } catch (scheduleFailure: Throwable) {
            failPendingRequest(player.uniqueId, request, scheduleFailure)
        }
        return request
    }

    private fun failPendingRequest(playerId: UUID, request: PendingAimRequest, throwable: Throwable) {
        if (pendingRequests.remove(playerId, request)) {
            request.fail(throwable)
        }
    }

    private fun handleConfirmation(player: Player, isConfirmed: Boolean): Boolean {
        val request = pendingRequests[player.uniqueId] ?: return false
        if (!isConfirmed) {
            if (!sendDataPacket(player, PacketType.AimConfirm) { writeBoolean(false) }) {
                failPendingRequest(player.uniqueId, request, AimPacketException("瞄准取消发送失败"))
                return true
            }
            failPendingRequest(player.uniqueId, request, PlayerCancelledException())
            return true
        }
        if (request.lifecycle.isConfirmed()) return true
        if (!request.lifecycle.confirm()) return true
        if (!sendDataPacket(player, PacketType.AimConfirm) { writeBoolean(true) }) {
            failPendingRequest(player.uniqueId, request, AimPacketException("瞄准确认发送失败"))
        }
        return true
    }

    private fun cleanupRequest(player: Player) {
        val request = pendingRequests[player.uniqueId] ?: return
        failPendingRequest(player.uniqueId, request, PlayerCancelledException())
    }

    private inline fun sendDataPacket(
        player: Player,
        type: PacketType,
        block: ByteArrayDataOutput.() -> Unit = {},
    ): Boolean {
        debug { "SendPacket Send: $type" }
        return try {
            val output = ByteStreams.newDataOutput().apply {
                writeInt(type.header)
                block()
            }
            val payload = output.toByteArray()
            if (payload.size > MAX_PLUGIN_MESSAGE_BYTES) {
                warning("拒绝向玩家 ${player.name} 发送过大的 $type 数据包，长度=${payload.size}")
                return false
            }
            player.sendPluginMessage(
                BukkitPlugin.getInstance(),
                CHANNEL_NAME,
                payload
            )
            true
        } catch (ex: Exception) {
            warning("给玩家 ${player.name} 发送数据包失败: ${ex.message}")
            false
        }
    }

    /* 消息接收处理器 */
    private class MessageReceiver : PluginMessageListener {
        override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
            if (channel != CHANNEL_NAME) return
            if (message.size < Int.SIZE_BYTES || message.size > MAX_PLUGIN_MESSAGE_BYTES) {
                warning("忽略来自 ${player.name} 的畸形插件消息，长度=${message.size}")
                return
            }

            val input = ByteStreams.newDataInput(message)
            try {
                val header = input.readInt()
                debug { "SendPacket Receive: $header" }
                when (header) {
                    PacketType.AimResponse.header -> handleAimResponse(player, input)
                    else -> warning("收到未知数据包类型: $header")
                }
            } catch (ex: Exception) {
                warning("处理来自 ${player.name} 的数据包时出错: ${ex.message ?: ex.javaClass.simpleName}")
            }
        }

        private fun handleAimResponse(player: Player, input: ByteArrayDataInput) {
            val request = pendingRequests[player.uniqueId] ?: return
            val payload = try {
                AimResponseCodec.decodeLegacy(input)
            } catch (ex: Exception) {
                warning("解析玩家 ${player.name} 的瞄准数据包失败: ${ex.message ?: ex.javaClass.simpleName}")
                failPendingRequest(player.uniqueId, request, AimPacketException("瞄准响应格式错误", ex))
                return
            }

            if (payload.skillId != request.wireSkillId) {
                warning("忽略玩家 ${player.name} 的过期瞄准响应")
                return
            }
            if (!request.lifecycle.isConfirmed()) {
                warning("忽略玩家 ${player.name} 未经确认的瞄准响应")
                return
            }
            if (!payload.isFinite()) {
                warning("玩家 ${player.name} 发送了非有限值瞄准坐标")
                failPendingRequest(player.uniqueId, request, InvalidAimResponseException("瞄准坐标不是有限值"))
                return
            }

            val playerLocation = player.location
            if (!playerLocation.hasFiniteCoordinates()) {
                failPendingRequest(player.uniqueId, request, InvalidAimResponseException("玩家坐标不是有限值"))
                return
            }
            val distanceSquared = payload.distanceSquared(playerLocation)
            val allowedDistanceSquared = request.maxDistance * request.maxDistance
            if (!distanceSquared.isFinite() || distanceSquared > allowedDistanceSquared) {
                warning("玩家 ${player.name} 向服务器发送了超远释放 ${request.skillId} 技能数据包")
                failPendingRequest(player.uniqueId, request, InvalidAimResponseException("瞄准位置超过允许距离"))
                return
            }

            val location = payload.toLocation(player)
            if (pendingRequests.remove(player.uniqueId, request) && request.lifecycle.complete()) {
                request.future.complete(AimInfo(player, location, request.skillId))
            }
        }
    }

    /* 数据类与旧协议编解码 */
    internal data class PendingAimRequest(
        val requestId: Long,
        val skillId: String,
        val wireSkillId: String,
        val maxDistance: Double,
        val future: CompletableFuture<AimInfo>,
        val lifecycle: AimRequestLifecycle = AimRequestLifecycle(),
    ) {
        fun fail(throwable: Throwable) {
            if (lifecycle.cancel()) {
                future.completeExceptionally(throwable)
            }
        }
    }

    internal data class AimResponsePayload(
        val skillId: String,
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Float,
        val pitch: Float,
    ) {
        fun isFinite(): Boolean {
            return x.isFinite() && y.isFinite() && z.isFinite() && yaw.isFinite() && pitch.isFinite()
        }

        fun distanceSquared(location: Location): Double {
            val dx = x - location.x
            val dy = y - location.y
            val dz = z - location.z
            return dx * dx + dy * dy + dz * dz
        }

        fun toLocation(player: Player): Location {
            return Location(player.world, x, y, z, yaw, pitch)
        }
    }

    internal object AimResponseCodec {
        /** 保持现有 header=4 的 legacy 载荷顺序不变。 */
        fun decodeLegacy(input: ByteArrayDataInput): AimResponsePayload {
            val skillId = input.readUTF()
            if (skillId.isBlank() || skillId.length > AimRequestProtocol.MAX_SKILL_ID_LENGTH) {
                throw AimPacketException("技能标识无效")
            }
            return AimResponsePayload(
                skillId = skillId,
                x = input.readDouble(),
                y = input.readDouble(),
                z = input.readDouble(),
                yaw = input.readFloat(),
                pitch = input.readFloat(),
            )
        }
    }

    private fun Location.hasFiniteCoordinates(): Boolean {
        return x.isFinite() && y.isFinite() && z.isFinite() && yaw.isFinite() && pitch.isFinite()
    }

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

    class AimSupersededException : RuntimeException("瞄准请求已被新的请求替代")

    class InvalidAimResponseException(message: String) : RuntimeException(message)

    class AimPacketException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
}