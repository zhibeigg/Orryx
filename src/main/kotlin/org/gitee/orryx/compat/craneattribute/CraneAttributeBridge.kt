package org.gitee.orryx.compat.craneattribute

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.OrryxDamageEvents
import org.gitee.orryx.compat.IAttributeBridge
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.gitee.orryx.utils.doDamage
import taboolib.module.kether.ScriptContext

class CraneAttributeBridge : IAttributeBridge {

    private val apiClass = Class.forName("cn.org.bukkit.craneattribute.api.AttributeAPI")
    private val apiInstance = apiClass.getField("INSTANCE").get(null)

    private val getAttrDataMethod = apiClass.getMethod("getAttrData", LivingEntity::class.java)
    private val addAttributeSourceMethod = apiClass.getMethod(
        "addAttributeSource",
        Class.forName("cn.org.bukkit.craneattribute.api.attribute.data.AttributeData"),
        String::class.java,
        List::class.java
    )
    private val removeAttributeSourceMethod = apiClass.getMethod(
        "removeAttributeSource",
        Class.forName("cn.org.bukkit.craneattribute.api.attribute.data.AttributeData"),
        String::class.java
    )
    private val updateAttributeMethod = apiClass.getMethod("updateAttribute", LivingEntity::class.java)

    override fun addAttribute(entity: LivingEntity, key: String, value: List<String>, timeout: Long) {
        val data = getAttrDataMethod.invoke(apiInstance, entity)
        addAttributeSourceMethod.invoke(apiInstance, data, key, value)
        updateAttributeMethod.invoke(apiInstance, entity)
        if (timeout != -1L) {
            SimpleTimeoutTask.createSimpleTask(timeout) {
                removeAttribute(entity, key)
            }
        }
    }

    override fun removeAttribute(entity: LivingEntity, key: String) {
        val data = getAttrDataMethod.invoke(apiInstance, entity)
        removeAttributeSourceMethod.invoke(apiInstance, data, key)
        updateAttributeMethod.invoke(apiInstance, entity)
    }

    override fun damage(
        attacker: LivingEntity,
        target: LivingEntity,
        damage: Double,
        type: DamageType,
        context: ScriptContext?
    ) {
        val bukkitEvent = EntityDamageByEntityEvent(attacker, target, DamageCause.CUSTOM, damage)
        val event = OrryxDamageEvents.Pre(attacker, target, damage, bukkitEvent, type, context)
        if (event.call()) {
            doDamage(event.attacker as? LivingEntity, event.defender as? LivingEntity ?: return, event.event!!.cause, event.damage)
            OrryxDamageEvents.Post(attacker, target, event.damage, bukkitEvent, event.type, false, context).call()
        }
    }

    override fun update(entity: LivingEntity) {
        updateAttributeMethod.invoke(apiInstance, entity)
    }
}
