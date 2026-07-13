package org.gitee.orryx.api

import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.OrryxDamageEvents
import org.gitee.orryx.api.interfaces.IBlockStatus
import org.gitee.orryx.api.interfaces.IProfileAPI
import org.gitee.orryx.api.interfaces.ITimedStatus
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.utils.VersionCompat
import org.gitee.orryx.utils.orryxProfileTo
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submit
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.function.Function

internal data class TimedStatusApplication(
    val previousExpiresAt: Long,
    val appliedVersion: Long,
)

class ProfileAPI: IProfileAPI {

    override fun <T> modifyProfile(player: Player, function: Function<IPlayerProfile, T>): CompletableFuture<T?> {
        return player.orryxProfileTo {
            function.apply(it)
        }
    }

    override fun superBody(): ITimedStatus = superBodyStatus
    override fun invincible(): ITimedStatus = invincibleStatus
    override fun superFoot(): ITimedStatus = superFootStatus
    override fun silence(): ITimedStatus = silenceStatus
    override fun block(): IBlockStatus = blockStatus

    companion object {

        private val superBodyModifier: AttributeModifier by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            VersionCompat.createAttributeModifier("Orryx@SuperBody", 99999.0, AttributeModifier.Operation.ADD_NUMBER)
        }

        // ==================== TimedStatusImpl ====================

        private class TimedStatusImpl(
            private val map: ConcurrentHashMap<UUID, TimedInfo> = ConcurrentHashMap(),
            private val onActivate: ((Player) -> Unit)? = null,
            private val onDeactivate: ((Player) -> Unit)? = null,
        ) : ITimedStatus {

            class TimedInfo(
                @Volatile var expiresAt: Long,
                @Volatile var task: taboolib.common.platform.service.PlatformExecutor.PlatformTask? = null,
            )

            private val versions = ConcurrentHashMap<UUID, Long>()
            private val versionSequence = AtomicLong()

            override fun isActive(player: Player): Boolean {
                return (map[player.uniqueId]?.expiresAt ?: return false) > System.currentTimeMillis()
            }

            override fun countdown(player: Player): Long {
                return positiveDifference(map[player.uniqueId]?.expiresAt ?: return 0L, System.currentTimeMillis())
            }

            override fun set(player: Player, timeout: Long) {
                applyWithReceipt(player, timeout)
            }

            fun applyWithReceipt(player: Player, timeout: Long): TimedStatusApplication {
                requireMainThread("TimedStatus.set")
                val playerId = player.uniqueId
                val now = System.currentTimeMillis()
                val requestedExpiresAt = saturatedAdd(now, timeout.coerceAtLeast(0L))
                val previousExpiresAt = map[playerId]?.expiresAt ?: 0L
                val wasActive = previousExpiresAt > now
                val expiresAt = maxOf(previousExpiresAt, requestedExpiresAt)
                map.remove(playerId)?.task?.cancel()
                if (expiresAt > now) {
                    val info = TimedInfo(expiresAt)
                    map[playerId] = info
                    if (!wasActive) onActivate?.invoke(player)
                    scheduleDeactivation(player, info)
                } else if (wasActive) {
                    onDeactivate?.invoke(player)
                }
                val version = markVersion(playerId)
                return TimedStatusApplication(previousExpiresAt, version)
            }

            fun restore(player: Player, application: TimedStatusApplication) {
                requireMainThread("TimedStatus.restore")
                val playerId = player.uniqueId
                if (versions[playerId] != application.appliedVersion) return
                val now = System.currentTimeMillis()
                val wasActive = isActive(player)
                map.remove(playerId)?.task?.cancel()
                if (application.previousExpiresAt > now) {
                    val info = TimedInfo(application.previousExpiresAt)
                    map[playerId] = info
                    if (!wasActive) onActivate?.invoke(player)
                    scheduleDeactivation(player, info)
                } else if (wasActive) {
                    onDeactivate?.invoke(player)
                }
                markVersion(playerId)
            }

            override fun cancel(player: Player) {
                cancelWithReceipt(player)
            }

            fun cancelWithReceipt(player: Player): TimedStatusApplication {
                requireMainThread("TimedStatus.cancel")
                val playerId = player.uniqueId
                val previousExpiresAt = map[playerId]?.expiresAt ?: 0L
                map.remove(playerId)?.let {
                    it.task?.cancel()
                    onDeactivate?.invoke(player)
                }
                return TimedStatusApplication(previousExpiresAt, markVersion(playerId))
            }

            override fun add(player: Player, timeout: Long) {
                requireMainThread("TimedStatus.add")
                if (timeout < 0L) return reduce(player, absoluteAmount(timeout))
                if (timeout == 0L) return
                val now = System.currentTimeMillis()
                val wasActive = isActive(player)
                val info = map.compute(player.uniqueId) { _, previous ->
                    val value = previous ?: TimedInfo(now)
                    value.expiresAt = saturatedAdd(maxOf(value.expiresAt, now), timeout)
                    value.task?.cancel()
                    value
                } ?: return
                if (!wasActive) onActivate?.invoke(player)
                scheduleDeactivation(player, info)
                markVersion(player.uniqueId)
            }

            override fun reduce(player: Player, timeout: Long) {
                requireMainThread("TimedStatus.reduce")
                if (timeout < 0L) return add(player, absoluteAmount(timeout))
                val info = map[player.uniqueId] ?: return
                info.expiresAt = saturatedSubtract(info.expiresAt, timeout)
                info.task?.cancel()
                if (info.expiresAt <= System.currentTimeMillis()) {
                    if (map.remove(player.uniqueId, info)) onDeactivate?.invoke(player)
                } else {
                    scheduleDeactivation(player, info)
                }
                markVersion(player.uniqueId)
            }

            private fun scheduleDeactivation(player: Player, info: TimedInfo) {
                if (onDeactivate == null) return
                val delayMillis = positiveDifference(info.expiresAt, System.currentTimeMillis())
                val delayTicks = (delayMillis / 50L).coerceAtMost(Long.MAX_VALUE - 1L) + 1L
                info.task = submit(delay = delayTicks) {
                    if (System.currentTimeMillis() >= info.expiresAt && map.remove(player.uniqueId, info)) {
                        onDeactivate.invoke(player)
                    }
                }
            }

            fun cleanup(player: Player) {
                map.remove(player.uniqueId)?.task?.cancel()
                versions.remove(player.uniqueId)
            }

            private fun markVersion(player: UUID): Long {
                val version = versionSequence.incrementAndGet()
                versions[player] = version
                return version
            }

            private fun saturatedAdd(value: Long, amount: Long): Long {
                return if (amount > 0L && value > Long.MAX_VALUE - amount) Long.MAX_VALUE else value + amount
            }

            private fun saturatedSubtract(value: Long, amount: Long): Long {
                return if (amount > 0L && value < Long.MIN_VALUE + amount) Long.MIN_VALUE else value - amount
            }

            private fun positiveDifference(end: Long, start: Long): Long {
                if (end <= start) return 0L
                return if (start < 0L && end > Long.MAX_VALUE + start) Long.MAX_VALUE else end - start
            }
        }

        // ==================== BlockStatusImpl ====================

        private class BlockStatusImpl : IBlockStatus {

            class BlockInfo {
                val map = ConcurrentHashMap<DamageType, Task>()
                class Task(@Volatile var timeout: Long, val function: (OrryxDamageEvents.Pre) -> Unit)
            }

            val blockMap = ConcurrentHashMap<UUID, BlockInfo>()

            override fun isActive(player: Player, type: DamageType): Boolean {
                val task = blockMap[player.uniqueId]?.map?.get(type) ?: return false
                return task.timeout > System.currentTimeMillis()
            }

            override fun countdown(player: Player, type: DamageType): Long {
                val task = blockMap[player.uniqueId]?.map?.get(type) ?: return 0
                return positiveDifference(task.timeout, System.currentTimeMillis())
            }

            override fun set(player: Player, type: DamageType, timeout: Long, onSuccess: Consumer<OrryxDamageEvents.Pre>) {
                requireMainThread("BlockStatus.set")
                if (timeout <= 0L) return cancel(player, type)
                val info = blockMap.getOrPut(player.uniqueId) { BlockInfo() }
                val now = System.currentTimeMillis()
                val currentTimeout = info.map[type]?.timeout ?: 0L
                val expiresAt = when {
                    timeout <= 0L -> now
                    now > Long.MAX_VALUE - timeout -> Long.MAX_VALUE
                    else -> now + timeout
                }
                info.map[type] = BlockInfo.Task(maxOf(currentTimeout, expiresAt)) { onSuccess.accept(it) }
            }

            override fun cancel(player: Player, type: DamageType) {
                requireMainThread("BlockStatus.cancel")
                val info = blockMap[player.uniqueId] ?: return
                info.map.remove(type)
                if (info.map.isEmpty()) blockMap.remove(player.uniqueId, info)
            }

            override fun cancelAll(player: Player) {
                requireMainThread("BlockStatus.cancelAll")
                blockMap.remove(player.uniqueId)
            }

            override fun add(player: Player, type: DamageType, timeout: Long) {
                requireMainThread("BlockStatus.add")
                if (timeout < 0L) return reduce(player, type, absoluteAmount(timeout))
                if (timeout == 0L) return
                val info = blockMap.getOrPut(player.uniqueId) { BlockInfo() }
                val task = info.map.getOrPut(type) { BlockInfo.Task(0) { } }
                val now = System.currentTimeMillis()
                task.timeout = saturatedAdd(maxOf(task.timeout, now), timeout)
            }

            override fun reduce(player: Player, type: DamageType, timeout: Long) {
                requireMainThread("BlockStatus.reduce")
                if (timeout < 0L) return add(player, type, absoluteAmount(timeout))
                if (timeout == 0L) return
                val info = blockMap[player.uniqueId] ?: return
                val task = info.map[type] ?: return
                task.timeout = saturatedSubtract(task.timeout, timeout)
                if (task.timeout <= System.currentTimeMillis()) {
                    info.map.remove(type, task)
                    if (info.map.isEmpty()) {
                        blockMap.remove(player.uniqueId, info)
                    }
                }
            }

            fun cleanup(player: Player) {
                blockMap.remove(player.uniqueId)
            }
        }

        private fun requireMainThread(operation: String) {
            check(isPrimaryThread) { "$operation 必须在 Bukkit 主线程执行" }
        }

        private fun saturatedAdd(value: Long, amount: Long): Long {
            return if (amount > 0L && value > Long.MAX_VALUE - amount) Long.MAX_VALUE else value + amount
        }

        private fun saturatedSubtract(value: Long, amount: Long): Long {
            return if (amount > 0L && value < Long.MIN_VALUE + amount) Long.MIN_VALUE else value - amount
        }

        private fun positiveDifference(end: Long, start: Long): Long {
            if (end <= start) return 0L
            return if (start < 0L && end > Long.MAX_VALUE + start) Long.MAX_VALUE else end - start
        }

        private fun absoluteAmount(amount: Long): Long {
            return if (amount == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(amount)
        }

        // ==================== 状态实例 ====================

        private val superBodyStatus = TimedStatusImpl(
            onActivate = { player ->
                val attr = player.getAttribute(VersionCompat.GENERIC_KNOCKBACK_RESISTANCE)
                if (attr?.modifiers?.any { VersionCompat.matchesModifierName(it, "Orryx@SuperBody") } != true) {
                    attr?.addModifier(superBodyModifier)
                }
            },
            onDeactivate = { player ->
                player.getAttribute(VersionCompat.GENERIC_KNOCKBACK_RESISTANCE)?.removeModifier(superBodyModifier)
            }
        )

        private val invincibleStatus = TimedStatusImpl()
        private val superFootStatus = TimedStatusImpl()
        private val silenceStatus = TimedStatusImpl()
        private val blockStatus = BlockStatusImpl()

        internal fun applySilenceTransaction(player: Player, timeout: Long): TimedStatusApplication {
            return silenceStatus.applyWithReceipt(player, timeout)
        }

        internal fun cancelSilenceTransaction(player: Player): TimedStatusApplication {
            return silenceStatus.cancelWithReceipt(player)
        }

        internal fun restoreSilenceTransaction(player: Player, application: TimedStatusApplication) {
            silenceStatus.restore(player, application)
        }

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<IProfileAPI>(ProfileAPI())
        }

        @SubscribeEvent
        private fun damage(e: OrryxDamageEvents.Pre) {
            if (invincibleStatus.isActive(e.defenderPlayer() ?: return)) {
                e.isCancelled = true
            }
        }

        @SubscribeEvent
        private fun damage(e: EntityDamageEvent) {
            val player = e.entity as? Player ?: return
            if (e.cause != EntityDamageEvent.DamageCause.SUICIDE && e.cause != EntityDamageEvent.DamageCause.CUSTOM) {
                if (invincibleStatus.isActive(player)) {
                    e.isCancelled = true
                    return
                }
            }
            if (e.cause == EntityDamageEvent.DamageCause.FALL || e.cause == EntityDamageEvent.DamageCause.SUFFOCATION) {
                if (superBodyStatus.isActive(player) || superFootStatus.isActive(player)) {
                    e.isCancelled = true
                }
            }
        }

        @SubscribeEvent
        private fun block(e: OrryxDamageEvents.Pre) {
            val player = e.defenderPlayer() ?: return
            if (blockStatus.isActive(player, e.type)) {
                e.isCancelled = true
                blockStatus.blockMap[player.uniqueId]?.map?.get(e.type)?.function?.invoke(e)
            }
        }

        @SubscribeEvent
        private fun quit(e: PlayerQuitEvent) {
            superBodyStatus.cancel(e.player)
            invincibleStatus.cleanup(e.player)
            superFootStatus.cleanup(e.player)
            silenceStatus.cleanup(e.player)
            blockStatus.cleanup(e.player)
        }
    }
}
