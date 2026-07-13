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
import taboolib.common.platform.function.submit
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.Function

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

            class TimedInfo(var expiresAt: Long, var task: taboolib.common.platform.service.PlatformExecutor.PlatformTask? = null)

            override fun isActive(player: Player): Boolean {
                return (map[player.uniqueId]?.expiresAt ?: return false) > System.currentTimeMillis()
            }

            override fun countdown(player: Player): Long {
                return positiveDifference(map[player.uniqueId]?.expiresAt ?: return 0L, System.currentTimeMillis())
            }

            override fun set(player: Player, timeout: Long) {
                val now = System.currentTimeMillis()
                val expiresAt = saturatedAdd(now, timeout.coerceAtLeast(0L))
                val wasActive = isActive(player)
                val info = map.compute(player.uniqueId) { _, previous ->
                    (previous ?: TimedInfo(expiresAt)).also {
                        it.expiresAt = maxOf(it.expiresAt, expiresAt)
                        it.task?.cancel()
                    }
                } ?: return
                if (!wasActive && info.expiresAt > now) onActivate?.invoke(player)
                scheduleDeactivation(player, info)
            }

            override fun cancel(player: Player) {
                map.remove(player.uniqueId)?.let {
                    it.task?.cancel()
                    onDeactivate?.invoke(player)
                }
            }

            override fun add(player: Player, timeout: Long) {
                if (timeout <= 0L) return reduce(player, -timeout)
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
            }

            override fun reduce(player: Player, timeout: Long) {
                if (timeout < 0L) return add(player, if (timeout == Long.MIN_VALUE) Long.MAX_VALUE else -timeout)
                val info = map[player.uniqueId] ?: return
                info.expiresAt = saturatedSubtract(info.expiresAt, timeout)
                info.task?.cancel()
                if (info.expiresAt <= System.currentTimeMillis()) {
                    if (map.remove(player.uniqueId, info)) onDeactivate?.invoke(player)
                } else {
                    scheduleDeactivation(player, info)
                }
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
                class Task(var timeout: Long, val function: (OrryxDamageEvents.Pre) -> Unit)
            }

            val blockMap = ConcurrentHashMap<UUID, BlockInfo>()

            override fun isActive(player: Player, type: DamageType): Boolean {
                val task = blockMap[player.uniqueId]?.map?.get(type) ?: return false
                return task.timeout >= System.currentTimeMillis()
            }

            override fun countdown(player: Player, type: DamageType): Long {
                val task = blockMap[player.uniqueId]?.map?.get(type) ?: return 0
                return (task.timeout - System.currentTimeMillis()).coerceAtLeast(0)
            }

            override fun set(player: Player, type: DamageType, timeout: Long, onSuccess: Consumer<OrryxDamageEvents.Pre>) {
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
                blockMap[player.uniqueId]?.map?.remove(type)
            }

            override fun cancelAll(player: Player) {
                blockMap.remove(player.uniqueId)
            }

            override fun add(player: Player, type: DamageType, timeout: Long) {
                val info = blockMap.getOrPut(player.uniqueId) { BlockInfo() }
                val task = info.map.getOrPut(type) { BlockInfo.Task(0) { } }
                task.also {
                    if (it.timeout >= System.currentTimeMillis()) {
                        it.timeout += timeout
                    } else {
                        it.timeout = System.currentTimeMillis() + timeout
                    }
                }
            }

            override fun reduce(player: Player, type: DamageType, timeout: Long) {
                val info = blockMap[player.uniqueId] ?: return
                val task = info.map[type] ?: return
                task.timeout -= timeout
                if (task.timeout < System.currentTimeMillis()) {
                    info.map.remove(type)
                    if (info.map.isEmpty()) {
                        blockMap.remove(player.uniqueId)
                    }
                }
            }

            fun cleanup(player: Player) {
                blockMap.remove(player.uniqueId)
            }
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
