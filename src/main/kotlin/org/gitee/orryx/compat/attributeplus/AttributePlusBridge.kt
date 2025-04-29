package org.gitee.orryx.compat.attributeplus

import org.bukkit.entity.LivingEntity
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.compat.IAttributeBridge
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.serverct.ersha.api.AttributeAPI
import org.serverct.ersha.attribute.data.AttributeData
import taboolib.common.util.unsafeLazy
import taboolib.module.kether.ScriptContext

class AttributePlusBridge: IAttributeBridge {

    private val attributeMap by unsafeLazy { mutableMapOf<String, AttributeData>() }

    override fun addAttribute(entity: LivingEntity, key: String, value: List<String>, timeout: Long) {
        val source = AttributeAPI.createStaticAttributeSource(value)
        source.entity = entity
        source.attributeData?.let {
            attributeMap[key] = it
            AttributeAPI.addStaticAttributeSource(it, key, value)
        }
        SimpleTimeoutTask.createSimpleTask(timeout) {
            removeAttribute(entity, key)
        }
    }

    override fun removeAttribute(entity: LivingEntity, key: String) {
        val attribute = attributeMap[key] ?: return
        AttributeAPI.takeSourceAttribute(attribute, key)
    }

    override fun damage(attacker: LivingEntity, target: LivingEntity, damage: Double, type: DamageType, context: ScriptContext?) {
        AttributeAPI.runAttributeAttackEntity(attacker, target, damage, callBeforeEvent = true, callCancel = true)
    }

    override fun update(entity: LivingEntity) {
        AttributeAPI.updateAttribute(entity)
    }
}