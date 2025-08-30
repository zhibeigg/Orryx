package org.gitee.orryx.core.damage

import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.nodens.api.events.entity.NodensEntityDamageEvents
import org.gitee.nodens.core.attribute.Damage
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.OrryxDamageEvents
import org.gitee.orryx.utils.AstraXHeroPlugin
import org.gitee.orryx.utils.AttributePlusPlugin
import org.gitee.orryx.utils.NodensPlugin
import org.gitee.orryx.utils.transfer
import org.serverct.ersha.api.event.AttrEntityDamageBeforeEvent
import org.serverct.ersha.api.event.AttrEntityDamageEvent
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent

object ProxyDamageManager {

    val ignoreBukkit by lazy { NodensPlugin.isEnabled || AttributePlusPlugin.isEnabled || AstraXHeroPlugin.isEnabled}

    //Bukkit
    @SubscribeEvent(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun bukkit(e: EntityDamageByEntityEvent) {
        if (ignoreBukkit) return
        val event = OrryxDamageEvents.Pre(e.damager, e.entity, e.damage, e, transfer(e.cause))
        if (event.call()) {
            e.damage = event.damage
            OrryxDamageEvents.Post(e.damager, e.entity, e.damage, e, transfer(e.cause)).call()
        } else {
            e.isCancelled = true
        }
    }

    // AttributePlus
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
        OrryxDamageEvents.Post(e.attacker, e.target, e.targetDamage, null, DamageType.CUSTOM).call()
    }

    // Nodens
    @Ghost
    @SubscribeEvent(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun onNoDamage(e: NodensEntityDamageEvents.Pre) {
        val type = when(e.processor.damageType) {
            Damage.Physics.name.uppercase() -> DamageType.PHYSICS
            Damage.Magic.name.uppercase() -> DamageType.MAGIC
            Damage.Real.name.uppercase() -> DamageType.REAL
            Damage.Fire.name.uppercase() -> DamageType.FIRE
            else -> DamageType.CUSTOM
        }
        val event = OrryxDamageEvents.Pre(e.processor.attacker, e.processor.defender, e.processor.getFinalDamage(), null, type)
        event.origin = e
        if (!event.call()) {
            e.isCancelled = true
        }
    }

    @Ghost
    @SubscribeEvent(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun onNoDamage(e: NodensEntityDamageEvents.Post) {
        val type = when(e.processor.damageType) {
            Damage.Physics.name.uppercase() -> DamageType.PHYSICS
            Damage.Magic.name.uppercase() -> DamageType.MAGIC
            Damage.Real.name.uppercase() -> DamageType.REAL
            Damage.Fire.name.uppercase() -> DamageType.FIRE
            else -> DamageType.CUSTOM
        }
        OrryxDamageEvents.Post(e.processor.attacker, e.processor.defender, e.processor.getFinalDamage(), null, type, e.processor.crit).call()
    }
}