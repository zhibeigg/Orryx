package org.gitee.orryx.compat

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.OrryxDamageEvents
import org.gitee.orryx.utils.doDamage
import taboolib.common.platform.function.warning
import taboolib.module.kether.ScriptContext

class DefaultAttributeBridge: IAttributeBridge {

    override fun addAttribute(entity: LivingEntity, key: String, value: List<String>, timeout: Long) {
        warning("Not Found Attribute Plugin")
    }

    override fun removeAttribute(entity: LivingEntity, key: String) {
        warning("Not Found Attribute Plugin")
    }

    override fun damage(attacker: LivingEntity, target: LivingEntity, damage: Double, type: DamageType, context: ScriptContext?) {
        val bukkitEvent = EntityDamageByEntityEvent(attacker, target, DamageCause.CUSTOM, damage)
        val event = OrryxDamageEvents.Pre(attacker, target, damage, bukkitEvent, type, context)
        if (event.call()) {
            doDamage(event.attacker as? LivingEntity, event.victim as? LivingEntity ?: return, event.event!!.cause, event.damage)
            OrryxDamageEvents.Post(attacker, target, event.damage, bukkitEvent, event.type, context).call()
        }
    }

}