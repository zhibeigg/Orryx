package org.gitee.orryx.core.message.collider

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.collider.ICollider
import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.reload.Reload
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import taboolib.common.platform.service.PlatformExecutor
import java.util.UUID

/** OrryxMod 自定义碰撞箱实时同步入口。所有几何读取均在 Bukkit 主线程执行。 */
object ColliderSyncManager {

    private var settings = ColliderSyncSettings()
    private var task: PlatformExecutor.PlatformTask? = null
    private val registry = ColliderSyncRegistry(
        playerResolver = Bukkit::getPlayer,
        sender = PluginColliderPacketSender,
    )

    @Reload(1)
    @Awake(LifeCycle.ENABLE)
    private fun load() {
        val wasRunning = task != null
        if (wasRunning) registry.clearAll(sendRemove = true)
        settings = ColliderSyncSettings(
            defaultIntervalTicks = Orryx.config.getLong("OrryxMod.ColliderSync.IntervalTicks", 1L)
                .coerceIn(1L, 200L),
            maxTrackedPerViewer = Orryx.config.getInt("OrryxMod.ColliderSync.MaxTrackedPerViewer", 200)
                .coerceIn(1, 200),
            maxChecksPerTick = Orryx.config.getInt("OrryxMod.ColliderSync.MaxChecksPerTick", 1_000)
                .coerceAtLeast(1),
            maxPacketsPerTick = Orryx.config.getInt("OrryxMod.ColliderSync.MaxPacketsPerTick", 256)
                .coerceAtLeast(1),
        )
        registry.trim(settings.maxTrackedPerViewer)
        if (task == null) task = submit(period = 1L) { registry.tick(settings) }
    }

    @Awake(LifeCycle.DISABLE)
    private fun disable() {
        registry.clearAll(sendRemove = true)
        task?.cancel()
        task = null
    }

    fun show(
        viewer: Player,
        id: String,
        collider: ICollider<*>,
        color: ColliderRenderColor,
        realtime: Boolean = true,
        intervalTicks: Long = settings.defaultIntervalTicks,
    ) {
        if (!Bukkit.isPrimaryThread()) {
            submit { show(viewer, id, collider, color, realtime, intervalTicks) }
            return
        }
        registry.show(
            viewer = viewer,
            id = id,
            collider = collider,
            color = color,
            realtime = realtime,
            intervalTicks = intervalTicks.coerceIn(1L, 200L),
            settings = settings,
        )
    }

    fun update(viewer: Player, id: String, collider: ICollider<*>) {
        if (!Bukkit.isPrimaryThread()) {
            submit { update(viewer, id, collider) }
            return
        }
        registry.update(viewer, id, collider)
    }

    fun remove(viewer: Player, id: String) {
        if (!Bukkit.isPrimaryThread()) {
            submit { remove(viewer, id) }
            return
        }
        registry.remove(viewer.uniqueId, id, sendPacket = true, viewer = viewer)
    }

    @SubscribeEvent
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        registry.clearViewer(event.player.uniqueId, sendRemove = false)
    }

    @SubscribeEvent
    private fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        registry.clearViewer(event.player.uniqueId, sendRemove = true, viewer = event.player)
    }
}

internal data class ColliderSyncSettings(
    val defaultIntervalTicks: Long = 1L,
    val maxTrackedPerViewer: Int = 200,
    val maxChecksPerTick: Int = 1_000,
    val maxPacketsPerTick: Int = 256,
)

internal interface ColliderPacketSender {

    fun show(viewer: Player, snapshot: ColliderWireSnapshot): Boolean

    fun update(viewer: Player, snapshot: ColliderWireSnapshot): Boolean

    fun remove(viewer: Player, id: String): Boolean
}

private object PluginColliderPacketSender : ColliderPacketSender {

    override fun show(viewer: Player, snapshot: ColliderWireSnapshot): Boolean {
        return PluginMessageHandler.sendColliderShow(viewer, snapshot)
    }

    override fun update(viewer: Player, snapshot: ColliderWireSnapshot): Boolean {
        return PluginMessageHandler.sendColliderUpdate(viewer, snapshot)
    }

    override fun remove(viewer: Player, id: String): Boolean {
        return PluginMessageHandler.sendColliderRemovePacket(viewer, id)
    }
}

/** 可注入依赖的同步注册表，便于对预算、淘汰和生命周期进行单元测试。 */
internal class ColliderSyncRegistry(
    private val playerResolver: (UUID) -> Player?,
    private val sender: ColliderPacketSender,
) {

    private val entries = LinkedHashMap<ColliderKey, ColliderEntry>()
    private val realtimeOrder = ArrayList<ColliderKey>()
    private val viewerCounts = HashMap<UUID, Int>()
    private var realtimeCursor = 0
    private var currentTick = 0L

    fun show(
        viewer: Player,
        id: String,
        collider: ICollider<*>,
        color: ColliderRenderColor,
        realtime: Boolean,
        intervalTicks: Long,
        settings: ColliderSyncSettings,
    ): Boolean {
        if (!viewer.isOnline) return false
        val snapshot = buildSnapshot(id, collider, color) ?: return false
        val key = ColliderKey(viewer.uniqueId, id)
        val existing = entries[key]
        if (existing == null && viewerCounts.getOrDefault(viewer.uniqueId, 0) >= settings.maxTrackedPerViewer) {
            // OrryxMod 在达到 200 项时拒绝新 Show，因此必须先移除旧项再发送新项。
            evictOldest(viewer.uniqueId)
        }
        if (!sender.show(viewer, snapshot)) return false

        val entry = ColliderEntry(
            key = key,
            collider = collider,
            color = color,
            realtime = realtime,
            intervalTicks = intervalTicks.coerceIn(1L, 200L),
            viewerWorldId = viewer.world.uid,
            lastSnapshot = snapshot,
            nextUpdateTick = currentTick + intervalTicks.coerceIn(1L, 200L),
        )
        if (existing == null) {
            entries[key] = entry
            viewerCounts[viewer.uniqueId] = viewerCounts.getOrDefault(viewer.uniqueId, 0) + 1
        } else {
            entries[key] = entry
        }
        setRealtime(key, realtime)
        return true
    }

    fun update(viewer: Player, id: String, collider: ICollider<*>): Boolean {
        if (!viewer.isOnline) return false
        val key = ColliderKey(viewer.uniqueId, id)
        val existing = entries[key]
        val color = existing?.color ?: ColliderRenderColor.WHITE
        val snapshot = buildSnapshot(id, collider, color)
        if (snapshot == null) {
            if (existing != null) remove(viewer.uniqueId, id, sendPacket = true, viewer = viewer)
            return false
        }
        if (existing != null) existing.collider = collider
        val sent = sender.update(viewer, snapshot)
        if (sent && existing != null) {
            existing.lastSnapshot = snapshot
            existing.nextUpdateTick = currentTick + existing.intervalTicks
        }
        return sent
    }

    fun remove(
        viewerId: UUID,
        id: String,
        sendPacket: Boolean,
        viewer: Player? = null,
    ): Boolean {
        val key = ColliderKey(viewerId, id)
        val removed = removeEntry(key) ?: return false
        if (sendPacket) {
            val target = viewer ?: playerResolver(viewerId)
            if (target != null && target.isOnline) sender.remove(target, removed.key.id)
        }
        return true
    }

    fun clearViewer(viewerId: UUID, sendRemove: Boolean, viewer: Player? = null) {
        val keys = entries.keys.filter { it.viewerId == viewerId }
        keys.forEach { key -> remove(viewerId, key.id, sendRemove, viewer) }
    }

    fun clearAll(sendRemove: Boolean) {
        val snapshot = entries.keys.toList()
        snapshot.forEach { key -> remove(key.viewerId, key.id, sendRemove) }
        entries.clear()
        realtimeOrder.clear()
        viewerCounts.clear()
        realtimeCursor = 0
    }

    fun trim(maxTrackedPerViewer: Int) {
        val limit = maxTrackedPerViewer.coerceIn(1, 200)
        viewerCounts.keys.toList().forEach { viewerId ->
            while (viewerCounts.getOrDefault(viewerId, 0) > limit) evictOldest(viewerId)
        }
    }

    fun tick(settings: ColliderSyncSettings) {
        currentTick++
        trim(settings.maxTrackedPerViewer)
        if (realtimeOrder.isEmpty()) return

        var checks = 0
        var packets = 0
        val maximumVisits = realtimeOrder.size
        var visits = 0
        while (realtimeOrder.isNotEmpty() && visits < maximumVisits && checks < settings.maxChecksPerTick) {
            if (realtimeCursor >= realtimeOrder.size) realtimeCursor = 0
            val key = realtimeOrder[realtimeCursor]
            realtimeCursor = if (realtimeOrder.size == 1) 0 else (realtimeCursor + 1) % realtimeOrder.size
            visits++

            val entry = entries[key]
            if (entry == null || !entry.realtime) {
                removeRealtimeKey(key)
                continue
            }
            if (currentTick < entry.nextUpdateTick) continue
            checks++

            val viewer = playerResolver(key.viewerId)
            if (viewer == null || !viewer.isOnline) {
                removeEntry(key)
                continue
            }
            if (viewer.world.uid != entry.viewerWorldId || entry.collider.disable()) {
                sender.remove(viewer, key.id)
                removeEntry(key)
                continue
            }

            val snapshot = try {
                ColliderWireCodec.snapshot(key.id, entry.collider, entry.color)
            } catch (ex: Throwable) {
                warning("停止同步无效碰撞箱 ${key.id}: ${ex.message}")
                sender.remove(viewer, key.id)
                removeEntry(key)
                null
            }
            if (snapshot == null) {
                if (entries.containsKey(key)) {
                    sender.remove(viewer, key.id)
                    removeEntry(key)
                }
                continue
            }
            if (snapshot == entry.lastSnapshot) {
                entry.nextUpdateTick = currentTick + entry.intervalTicks
                continue
            }
            if (packets >= settings.maxPacketsPerTick) {
                entry.nextUpdateTick = currentTick + 1L
                break
            }
            if (sender.update(viewer, snapshot)) {
                entry.lastSnapshot = snapshot
                packets++
            }
            entry.nextUpdateTick = currentTick + entry.intervalTicks
        }
    }

    internal fun size(): Int = entries.size

    internal fun contains(viewerId: UUID, id: String): Boolean = entries.containsKey(ColliderKey(viewerId, id))

    private fun buildSnapshot(
        id: String,
        collider: ICollider<*>,
        color: ColliderRenderColor,
    ): ColliderWireSnapshot? {
        return try {
            ColliderWireCodec.snapshot(id, collider, color)
        } catch (ex: Throwable) {
            warning("无法同步碰撞箱 $id: ${ex.message}")
            null
        }
    }

    private fun evictOldest(viewerId: UUID) {
        val key = entries.keys.firstOrNull { it.viewerId == viewerId } ?: return
        val viewer = playerResolver(viewerId)
        if (viewer != null && viewer.isOnline) sender.remove(viewer, key.id)
        removeEntry(key)
    }

    private fun setRealtime(key: ColliderKey, realtime: Boolean) {
        if (realtime) {
            if (!realtimeOrder.contains(key)) realtimeOrder += key
        } else {
            removeRealtimeKey(key)
        }
    }

    private fun removeEntry(key: ColliderKey): ColliderEntry? {
        val removed = entries.remove(key) ?: return null
        removeRealtimeKey(key)
        val remaining = viewerCounts.getOrDefault(key.viewerId, 1) - 1
        if (remaining <= 0) viewerCounts.remove(key.viewerId) else viewerCounts[key.viewerId] = remaining
        return removed
    }

    private fun removeRealtimeKey(key: ColliderKey) {
        val index = realtimeOrder.indexOf(key)
        if (index < 0) return
        realtimeOrder.removeAt(index)
        if (index < realtimeCursor) realtimeCursor--
        if (realtimeCursor < 0 || realtimeCursor >= realtimeOrder.size) realtimeCursor = 0
    }

    private data class ColliderKey(
        val viewerId: UUID,
        val id: String,
    )

    private data class ColliderEntry(
        val key: ColliderKey,
        var collider: ICollider<*>,
        val color: ColliderRenderColor,
        val realtime: Boolean,
        val intervalTicks: Long,
        val viewerWorldId: UUID,
        var lastSnapshot: ColliderWireSnapshot,
        var nextUpdateTick: Long,
    )
}
