package org.gitee.orryx.core.damage

import ac.github.oa.api.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.OrryxDamageEvents
import org.gitee.orryx.utils.AttributePlusPlugin
import org.gitee.orryx.utils.OriginAttributePlugin
import org.gitee.orryx.utils.transfer
import org.serverct.ersha.api.event.AttrEntityDamageBeforeEvent
import org.serverct.ersha.api.event.AttrEntityDamageEvent
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent

object ProxyDamageManager {

    //Bukkit
    @SubscribeEvent(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun bukkit(e: EntityDamageByEntityEvent) {
        if (OriginAttributePlugin.isEnabled || AttributePlusPlugin.isEnabled) return
        val event = OrryxDamageEvents.Pre(e.damager, e.entity, e.damage, e, transfer(e.cause))
        if (event.call()) {
            e.damage = event.damage
            OrryxDamageEvents.Post(e.damager, e.entity, e.damage, e, transfer(e.cause)).call()
        } else {
            e.isCancelled = true
        }
    }

    //OriginAttribute
    @Ghost
    @SubscribeEvent(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun onOaDamage(e: EntityDamageEvent) {
        val type = when (e.damageMemory.cause.uppercase()) {
            "MAGIC" -> DamageType.MAGIC
            "PHYSICS" -> DamageType.PHYSICS
            "FIRE" -> DamageType.FIRE
            "SELF" -> DamageType.SELF
            "CONSOLE" -> DamageType.CONSOLE
            else -> DamageType.PHYSICS
        }
        if (e.isPre) {
            val event = OrryxDamageEvents.Pre(e.attacker, e.victim, e.damageMemory.totalDamage, e.damageMemory.event.origin, type)
            event.origin = e.damageMemory
            if (!event.call()) {
                e.isCancelled = true
            }
        } else {
            OrryxDamageEvents.Post(e.attacker, e.victim, e.damageMemory.totalDamage, e.damageMemory.event.origin, type).call()
        }
    }

    //AttributePlus
    @Ghost
    @SubscribeEvent(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun onApDamagePre(e: AttrEntityDamageBeforeEvent) {
        val type = if (e.handle.isSkillDamage) {
            DamageType.MAGIC
        } else {
            DamageType.PHYSICS
        }
        val event = OrryxDamageEvents.Pre(e.attacker, e.target, e.damage, null, type)
        event.origin = e
        if (!event.call()) {
            e.isCancelled = true
        }
    }

    @Ghost
    @SubscribeEvent(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun onApDamage(e: AttrEntityDamageEvent) {
        OrryxDamageEvents.Post(e.attacker, e.target, e.targetDamage, null, DamageType.CONSOLE).call()
    }

}