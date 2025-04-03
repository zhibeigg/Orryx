package org.gitee.orryx.api

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.gitee.orryx.api.interfaces.IProfileAPI
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.Schedule
import taboolib.common.util.unsafeLazy
import taboolib.platform.util.onlinePlayers
import java.util.*

class ProfileAPI: IProfileAPI {

    override fun isSuperBody(player: Player): Boolean {
        return (superBodyMap[player.uniqueId]?.timeout ?: return false) >= System.currentTimeMillis()
    }

    override fun setSuperBody(player: Player, timeout: Long) {
        superBodyMap.getOrPut(player.uniqueId) { SuperBodyInfo(0) }.timeout = System.currentTimeMillis() + timeout
        player.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK)?.addModifier(superBodyModifier)
    }

    override fun cancelSuperBody(player: Player) {
        superBodyMap.remove(player.uniqueId)?.apply {
            player.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK)?.removeModifier(superBodyModifier)
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
        player.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK)?.addModifier(superBodyModifier)
    }

    override fun reduceSuperBody(player: Player, timeout: Long) {
        superBodyMap[player.uniqueId]?.also {
            it.timeout -= timeout
            if (it.timeout < System.currentTimeMillis()) {
                superBodyMap.remove(player.uniqueId)
                player.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK)?.removeModifier(superBodyModifier)
            }
        }
    }

    override fun isSuperFoot(player: Player): Boolean {
        return (superFootMap[player.uniqueId]?.timeout ?: return false) >= System.currentTimeMillis()
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

    companion object {

        private val superBodyModifier: AttributeModifier by unsafeLazy { AttributeModifier("Orryx@SuperBody", 99999.0, AttributeModifier.Operation.ADD_NUMBER) }

        //霸体过期时间
        private val superBodyMap by unsafeLazy { hashMapOf<UUID, SuperBodyInfo>() }
        //免疫摔伤过期时间
        private val superFootMap by unsafeLazy { hashMapOf<UUID, SuperFootInfo>() }

        class SuperBodyInfo(var timeout: Long)
        class SuperFootInfo(var timeout: Long)

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

    }

}