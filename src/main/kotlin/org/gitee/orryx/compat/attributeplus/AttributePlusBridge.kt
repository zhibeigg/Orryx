package org.gitee.orryx.compat.attributeplus

import org.bukkit.entity.LivingEntity
import org.gitee.orryx.compat.IAttributeBridge
import org.serverct.ersha.api.AttributeAPI

class AttributePlusBridge: IAttributeBridge {

    override fun addAttribute(key: String, value: List<String>) {
        TODO("Not yet implemented")
    }

    override fun removeAttribute(key: String) {
        TODO("Not yet implemented")
    }

    override fun damage(attacker: LivingEntity, target: LivingEntity, damage: Double) {
        AttributeAPI.runAttributeAttackEntity(attacker, target, damage, callBeforeEvent = true, callCancel = true)
    }

}