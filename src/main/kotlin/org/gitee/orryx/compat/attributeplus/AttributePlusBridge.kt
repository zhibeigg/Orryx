package org.gitee.orryx.compat.attributeplus

import org.bukkit.entity.LivingEntity
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.compat.IAttributeBridge
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.serverct.ersha.api.AttributeAPI
import taboolib.module.kether.ScriptContext

class AttributePlusBridge: IAttributeBridge {

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
}