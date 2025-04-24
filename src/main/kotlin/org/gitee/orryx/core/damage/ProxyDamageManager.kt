package org.gitee.orryx.core.damage

import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.OrryxDamageEvents
import org.gitee.orryx.utils.AttributePlusPlugin
import org.gitee.orryx.utils.NodensPlugin
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
        if (NodensPlugin.isEnabled || AttributePlusPlugin.isEnabled) return
        val event = OrryxDamageEvents.Pre(e.damager, e.entity, e.damage, e, transfer(e.cause))
        if (event.call()) {
            e.damage = event.damage
            OrryxDamageEvents.Post(e.damager, e.entity, e.damage, e, transfer(e.cause)).call()
        } else {
            e.isCancelled = true
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