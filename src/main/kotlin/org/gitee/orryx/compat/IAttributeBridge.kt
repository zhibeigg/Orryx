package org.gitee.orryx.compat

import org.bukkit.entity.LivingEntity
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.compat.astraxhero.AstraXHeroBridge
import org.gitee.orryx.compat.attributeplus.AttributePlusBridge
import org.gitee.orryx.compat.nodens.NodensBridge
import org.gitee.orryx.utils.AstraXHeroPlugin
import org.gitee.orryx.utils.AttributePlusPlugin
import org.gitee.orryx.utils.NodensPlugin
import taboolib.common.util.unsafeLazy
import taboolib.module.kether.ScriptContext

/**
 * 属性系统兼容桥接接口。
 */
interface IAttributeBridge {

    companion object {

        val INSTANCE by unsafeLazy {
            when {
                AttributePlusPlugin.isEnabled -> AttributePlusBridge()
                NodensPlugin.isEnabled -> NodensBridge()
                AstraXHeroPlugin.isEnabled -> AstraXHeroBridge()
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
     * @param context 产生攻击的上下文
     * */
    fun damage(attacker: LivingEntity, target: LivingEntity, damage: Double, type: DamageType, context: ScriptContext? = null)

    /**
     * 更新实体属性。
     *
     * @param entity 需要更新的实体
     */
    fun update(entity: LivingEntity)
}
