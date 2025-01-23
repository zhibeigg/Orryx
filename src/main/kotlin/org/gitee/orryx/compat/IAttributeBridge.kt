package org.gitee.orryx.compat

import org.bukkit.entity.LivingEntity
import org.gitee.orryx.api.events.damage.DamageType
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

    /**
     * 添加临时属性
     * @param entity 获得属性的实体
     * @param key 属性标识键
     * @param value 属性信息
     * ```
     * "物理攻击: +99"
     * ```
     * @param timeout 持续时间 -1 为永久
     * */
    fun addAttribute(entity: LivingEntity, key: String, value: List<String>, timeout: Long = -1)

    /**
     * 移除属性
     * @param entity 移除属性的实体
     * @param key 属性标识键
     * */
    fun removeAttribute(entity: LivingEntity, key: String)

    /**
     * 攻击实体预输入数值，如有属性插件将计算属性值
     * @param attacker 攻击者
     * @param target 攻击目标
     * @param damage 攻击数值
     * @param type 攻击类型
     * */
    fun damage(attacker: LivingEntity, target: LivingEntity, damage: Double, type: DamageType)

}