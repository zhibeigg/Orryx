package org.gitee.orryx.compat

import org.bukkit.entity.LivingEntity
import org.gitee.orryx.compat.attributeplus.AttributePlusBridge
import org.gitee.orryx.compat.originattribute.OriginAttributeBridge
import org.gitee.orryx.utils.AttributePlusEnabled
import org.gitee.orryx.utils.OriginAttributeEnabled

interface IAttributeBridge {

    companion object {

        val INSTANCE by lazy {
            when {
                AttributePlusEnabled -> AttributePlusBridge()
                OriginAttributeEnabled -> OriginAttributeBridge()
                else -> DefaultAttributeBridge()
            }
        }

    }

    fun addAttribute(key: String, value: List<String>)

    fun removeAttribute(key: String)

    fun damage(attacker: LivingEntity, target: LivingEntity, damage: Double)

}