package org.gitee.orryx.api

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.OrryxDamageEvents
import org.gitee.orryx.api.interfaces.IProfileAPI
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.module.state.StateManager.statusData
import org.gitee.orryx.utils.orryxProfile
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.unsafeLazy
import taboolib.platform.util.onlinePlayers
import java.util.*
import java.util.concurrent.CompletableFuture

class ProfileAPI: IProfileAPI {

    override fun <T> modifyProfile(player: Player, function: (skill: IPlayerProfile) -> T): CompletableFuture<T?> {
        return player.orryxProfile(function)
    }

    override fun isSuperBody(player: Player): Boolean {
        return (superBodyMap[player.uniqueId]?.timeout ?: return false) >= System.currentTimeMillis()
    }

    override fun superBodyCountdown(player: Player): Long {
        return ((superBodyMap[player.uniqueId]?.timeout ?: return 0) - System.currentTimeMillis()).coerceAtLeast(0)
    }

    override fun setSuperBody(player: Player, timeout: Long) {
        superBodyMap.getOrPut(player.uniqueId) { SuperBodyInfo(0) }.timeout = System.currentTimeMillis() + timeout
        addKnockBackResistance(player)
    }

    override fun cancelSuperBody(player: Player) {
        superBodyMap.remove(player.uniqueId)?.apply {
            player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.removeModifier(superBodyModifier)
        }
    }

    override fun addSuperBody(player: Player, timeout: Long) {
        superBodyMap.getOrPut(player.uniqueId) { SuperBodyInfo(0) }.also {
            if (it.timeout >= System.currentTimeMillis()) {
                it.timeout += timeout
            } else {
                it.timeout = System.currentTimeMillis() + timeout
            }
        }
        addKnockBackResistance(player)
    }

    override fun reduceSuperBody(player: Player, timeout: Long) {
        superBodyMap[player.uniqueId]?.also {
            it.timeout -= timeout
            if (it.timeout < System.currentTimeMillis()) {
                superBodyMap.remove(player.uniqueId)
                player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.removeModifier(superBodyModifier)
            }
        }
    }

    private fun addKnockBackResistance(player: Player) {
        if (player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.modifiers?.contains(superBodyModifier) != true) {
            player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.addModifier(superBodyModifier)
        }
    }

    override fun isInvincible(player: Player): Boolean {
        return (invincibleMap[player.uniqueId]?.timeout ?: return false) >= System.currentTimeMillis()
    }

    override fun invincibleCountdown(player: Player): Long {
        return ((invincibleMap[player.uniqueId]?.timeout ?: return 0) - System.currentTimeMillis()).coerceAtLeast(0)
    }

    override fun setInvincible(player: Player, timeout: Long) {
        invincibleMap.getOrPut(player.uniqueId) { InvincibleInfo(0) }.timeout = System.currentTimeMillis() + timeout
    }

    override fun cancelInvincible(player: Player) {
        invincibleMap.remove(player.uniqueId)
    }

    override fun addInvincible(player: Player, timeout: Long) {
        invincibleMap.getOrPut(player.uniqueId) { InvincibleInfo(0) }.also {
            if (it.timeout >= System.currentTimeMillis()) {
                it.timeout += timeout
            } else {
                it.timeout = System.currentTimeMillis() + timeout
            }
        }
    }

    override fun reduceInvincible(player: Player, timeout: Long) {
        invincibleMap[player.uniqueId]?.also {
            it.timeout -= timeout
            if (it.timeout < System.currentTimeMillis()) {
                invincibleMap.remove(player.uniqueId)
            }
        }
    }

    override fun isSuperFoot(player: Player): Boolean {
        return (superFootMap[player.uniqueId]?.timeout ?: return false) >= System.currentTimeMillis()
    }

    override fun superFootCountdown(player: Player): Long {
        return ((superFootMap[player.uniqueId]?.timeout ?: return 0) - System.currentTimeMillis()).coerceAtLeast(0)
    }

    override fun setSuperFoot(player: Player, timeout: Long) {
        superFootMap.getOrPut(player.uniqueId) { SuperFootInfo(0) }.timeout = System.currentTimeMillis() + timeout
    }

    override fun cancelSuperFoot(player: Player) {
        superFootMap.remove(player.uniqueId)
    }

    override fun addSuperFoot(player: Player, timeout: Long) {
        superFootMap.getOrPut(player.uniqueId) { SuperFootInfo(0) }.also {
            if (it.timeout >= System.currentTimeMillis()) {
                it.timeout += timeout
            } else {
                it.timeout = System.currentTimeMillis() + timeout
            }
        }
    }

    override fun reduceSuperFoot(player: Player, timeout: Long) {
        superFootMap[player.uniqueId]?.also {
            it.timeout -= timeout
            if (it.timeout < System.currentTimeMillis()) {
                superFootMap.remove(player.uniqueId)
            }
        }
    }

    override fun isBlock(player: Player): Boolean {
        return (blockMap[player.uniqueId]?.timeout ?: return false) >= System.currentTimeMillis()
    }

    override fun blockCountdown(player: Player): Long {
        return ((blockMap[player.uniqueId]?.timeout ?: return 0) - System.currentTimeMillis()).coerceAtLeast(0)
    }

    override fun setBlock(player: Player, timeout: Long) {
        blockMap.getOrPut(player.uniqueId) { BlockInfo(0) }.timeout = System.currentTimeMillis() + timeout
    }

    override fun cancelBlock(player: Player) {
        blockMap.remove(player.uniqueId)
    }

    override fun addBlock(player: Player, timeout: Long) {
        blockMap.getOrPut(player.uniqueId) { BlockInfo(0) }.also {
            if (it.timeout >= System.currentTimeMillis()) {
                it.timeout += timeout
            } else {
                it.timeout = System.currentTimeMillis() + timeout
            }
        }
    }

    override fun reduceBlock(player: Player, timeout: Long) {
        blockMap[player.uniqueId]?.also {
            it.timeout -= timeout
            if (it.timeout < System.currentTimeMillis()) {
                blockMap.remove(player.uniqueId)
            }
        }
    }

    override fun isSilence(player: Player): Boolean {
        return (silenceMap[player.uniqueId]?.timeout ?: return false) >= System.currentTimeMillis()
    }

    override fun silenceCountdown(player: Player): Long {
        return ((silenceMap[player.uniqueId]?.timeout ?: return 0) - System.currentTimeMillis()).coerceAtLeast(0)
    }

    override fun setSilence(player: Player, timeout: Long) {
        silenceMap.getOrPut(player.uniqueId) { SilenceInfo(0) }.timeout = System.currentTimeMillis() + timeout
    }

    override fun cancelSilence(player: Player) {
        silenceMap.remove(player.uniqueId)
    }

    override fun addSilence(player: Player, timeout: Long) {
        silenceMap.getOrPut(player.uniqueId) { SilenceInfo(0) }.also {
            if (it.timeout >= System.currentTimeMillis()) {
                it.timeout += timeout
            } else {
                it.timeout = System.currentTimeMillis() + timeout
            }
        }
    }

    override fun reduceSilence(player: Player, timeout: Long) {
        silenceMap[player.uniqueId]?.also {
            it.timeout -= timeout
            if (it.timeout < System.currentTimeMillis()) {
                silenceMap.remove(player.uniqueId)
            }
        }
    }

    companion object {

        private val superBodyModifier: AttributeModifier by unsafeLazy { AttributeModifier("Orryx@SuperBody", 99999.0, AttributeModifier.Operation.ADD_NUMBER) }

        // 霸体过期时间
        private val superBodyMap by unsafeLazy { hashMapOf<UUID, SuperBodyInfo>() }
        // 无敌过期时间
        private val invincibleMap by unsafeLazy { hashMapOf<UUID, InvincibleInfo>() }
        // 免疫摔伤过期时间
        private val superFootMap by unsafeLazy { hashMapOf<UUID, SuperFootInfo>() }
        // 格挡过期时间
        private val blockMap by unsafeLazy { hashMapOf<UUID, BlockInfo>() }
        // 沉默过期时间
        private val silenceMap by unsafeLazy { hashMapOf<UUID, SilenceInfo>() }

        class SuperBodyInfo(var timeout: Long)
        class InvincibleInfo(var timeout: Long)
        class SuperFootInfo(var timeout: Long)
        class BlockInfo(var timeout: Long)
        class SilenceInfo(var timeout: Long)

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<IProfileAPI>(ProfileAPI())
        }

        @Schedule(async = false, period = 1)
        private fun check() {
            onlinePlayers.forEach {
                if (!Orryx.api().profileAPI.isSuperBody(it)) {
                    Orryx.api().profileAPI.cancelSuperBody(it)
                }
            }
        }

        @SubscribeEvent
        private fun drop(e: EntityDamageEvent) {
            val player = e.entity as? Player ?: return
            if (e.cause != EntityDamageEvent.DamageCause.SUICIDE) {
                if (Orryx.api().profileAPI.isInvincible(player)) {
                    e.isCancelled = true
                    return
                }
            }
            if (e.cause == EntityDamageEvent.DamageCause.FALL || e.cause == EntityDamageEvent.DamageCause.SUFFOCATION) {
                if (Orryx.api().profileAPI.isSuperBody(player) || Orryx.api().profileAPI.isSuperFoot(player)) {
                    e.isCancelled = true
                }
            }
        }

        @SubscribeEvent
        private fun block(e: OrryxDamageEvents.Pre) {
            val player = e.victimPlayer() ?: return
            if (e.type == DamageType.PHYSICS && Orryx.api().profileAPI.isBlock(player)) {
                e.isCancelled = true
                player.statusData()
            }
        }

        @SubscribeEvent
        private fun quit(e: PlayerQuitEvent) {
            Orryx.api().profileAPI.apply {
                cancelSuperBody(e.player)
                cancelSuperFoot(e.player)
                cancelInvincible(e.player)
                cancelBlock(e.player)
                cancelSilence(e.player)
            }
        }

    }

}