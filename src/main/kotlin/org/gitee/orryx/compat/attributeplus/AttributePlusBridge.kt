package org.gitee.orryx.compat.attributeplus

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.compat.IAttributeBridge
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.gitee.orryx.core.kether.actions.DamageActions.Default
import org.gitee.orryx.utils.AttributePlusPlugin
import org.gitee.orryx.utils.doDamage
import org.serverct.ersha.api.AttributeAPI
import org.serverct.ersha.attribute.AttributeHandle
import org.serverct.ersha.attribute.data.AttributeData
import org.serverct.ersha.attribute.data.AttributeSource
import taboolib.module.kether.ScriptContext

class AttributePlusBridge: IAttributeBridge {

    companion object {

        const val BRIDGE_TAG = "orryx@bridge"
    }

    override fun addAttribute(entity: LivingEntity, key: String, value: List<String>, timeout: Long) {
        val data = AttributeAPI.getAttrData(entity)
        AttributeAPI.addSourceAttribute(data, key, value)
        data.updateAttribute()
        if (timeout != -1L) {
            SimpleTimeoutTask.createSimpleTask(timeout) {
                removeAttribute(entity, key)
            }
        }
    }

    override fun removeAttribute(entity: LivingEntity, key: String) {
        val data = AttributeAPI.getAttrData(entity)
        AttributeAPI.takeSourceAttribute(data, key)
    }

    override fun damage(attacker: LivingEntity, target: LivingEntity, damage: Double, type: DamageType, context: ScriptContext?) {
        AttributeAPI.runAttributeAttackEntity(attacker, target, damage, callBeforeEvent = true, callCancel = true)
    }

    override fun update(entity: LivingEntity) {
        AttributeAPI.updateAttribute(entity)
    }

    fun apAttack(attacker: LivingEntity, target: LivingEntity, reset: Boolean = false, attributes: List<String>): Double {
        var damage = 0.0

        val data: AttributeData = if (reset) {
            AttributeData.create(attacker)
        } else {
            AttributeAPI.getAttrData(attacker)
        }

        data.operationAttribute(
            AttributeAPI.getAttributeSource(attributes),
            AttributeSource.OperationType.ADD,
            BRIDGE_TAG
        )

        val event =
            EntityDamageByEntityEvent(attacker, target, EntityDamageEvent.DamageCause.CUSTOM, 0.0)

        val handle = AttributeHandle(data, AttributeAPI.getAttrData(target))
            .init(event, isProjectile = false, isSkillDamage = true)
            .handleAttackOrDefenseAttribute()

        if (!event.isCancelled && !handle.isCancelled) {
            val finalDamage = handle.getDamage(attacker)

            handle.sendAttributeMessage()
            doDamage(attacker, target, event, finalDamage)
            damage += finalDamage

            if (handle.getDamage(target) > 0.0) {
                doDamage(target, attacker, event, finalDamage)
            }
        }

        data.takeApiAttribute(BRIDGE_TAG)
        return damage
    }
}