package org.gitee.orryx.compat.originattribute

import ac.github.oa.api.OriginAttributeAPI
import ac.github.oa.api.event.entity.EntityDamageEvent
import ac.github.oa.api.event.entity.ProxyDamageEvent
import ac.github.oa.internal.base.enums.PriorityEnum
import ac.github.oa.internal.core.attribute.AttributeData
import ac.github.oa.internal.core.attribute.impl.Damage
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.compat.IAttributeBridge
import org.gitee.orryx.utils.doDamage

class OriginAttributeBridge: IAttributeBridge {

    private val defaultCause by lazy { DamageCause.ENTITY_ATTACK }

    override fun addAttribute(entity: LivingEntity, key: String, value: List<String>, timeout: Long) {
        val data = Data(timeout)
        data.merge(OriginAttributeAPI.loadList(value))
        OriginAttributeAPI.setExtra(entity.uniqueId, key, data)
    }

    override fun removeAttribute(entity: LivingEntity, key: String) {
        OriginAttributeAPI.removeExtra(entity.uniqueId, key)
    }

    override fun damage(attacker: LivingEntity, target: LivingEntity, damage: Double, type: DamageType) {
        val event = ProxyDamageEvent(EntityDamageByEntityEvent(attacker, target, defaultCause, 0.0))
        val context = event.createDamageContext()

        context.vigor = damage
        context.cause = event.customCause

        when (type) {
            DamageType.PHYSICS -> {
                context.addDamage(Damage.physical, damage)
            }
            DamageType.MAGIC -> {
                context.addDamage(Damage.magic, damage)
            }
            DamageType.REAL -> {
                context.addDamage(Damage.real, damage)
            }
            else -> {
                context.addDamage(type, damage)
            }
        }

        if (EntityDamageEvent(context, PriorityEnum.PRE).call()) {
            OriginAttributeAPI.callDamage(context)
            if (EntityDamageEvent(context, PriorityEnum.POST).call()) {
                doDamage(attacker, target, DamageCause.CUSTOM, context.totalDamage.coerceAtLeast(0.0))
            }
        }
    }

    class Data(val timeout: Long) : AttributeData() {

        val create = System.currentTimeMillis()

        val end: Long
            get() = timeout + create

        val countdown: Long
            get() = (end - System.currentTimeMillis()).coerceAtLeast(0)

        override val isValid: Boolean
            get() = timeout == -1L || countdown > 0L

    }

}