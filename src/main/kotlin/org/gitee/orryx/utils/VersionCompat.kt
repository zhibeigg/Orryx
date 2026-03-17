package org.gitee.orryx.utils

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import taboolib.module.nms.MinecraftVersion
import java.util.*

/**
 * 版本兼容工具类
 * 处理 1.21.2+ Attribute 枚举重命名和 AttributeModifier 构造函数变更
 */
object VersionCompat {

    /**
     * 兼容获取 Attribute 枚举
     * 1.21.2+ 移除了 GENERIC_ 前缀，例如 GENERIC_MAX_HEALTH -> MAX_HEALTH
     */
    fun getAttribute(name: String): Attribute? {
        return try {
            Attribute.valueOf(name)
        } catch (_: IllegalArgumentException) {
            // 尝试去掉 GENERIC_ 前缀（旧名 -> 新名）
            val withoutPrefix = name.removePrefix("GENERIC_")
            try {
                Attribute.valueOf(withoutPrefix)
            } catch (_: IllegalArgumentException) {
                // 尝试加上 GENERIC_ 前缀（新名 -> 旧名）
                try {
                    Attribute.valueOf("GENERIC_$name")
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }
    }

    val GENERIC_MAX_HEALTH: Attribute by lazy { getAttribute("GENERIC_MAX_HEALTH") ?: getAttribute("MAX_HEALTH")!! }
    val GENERIC_MOVEMENT_SPEED: Attribute by lazy { getAttribute("GENERIC_MOVEMENT_SPEED") ?: getAttribute("MOVEMENT_SPEED")!! }
    val GENERIC_KNOCKBACK_RESISTANCE: Attribute by lazy { getAttribute("GENERIC_KNOCKBACK_RESISTANCE") ?: getAttribute("KNOCKBACK_RESISTANCE")!! }

    /**
     * 兼容创建 AttributeModifier
     * 1.21+ 弃用 (String, Double, Operation) 构造函数，要求使用 (NamespacedKey, Double, Operation) 版本
     */
    fun createAttributeModifier(name: String, amount: Double, operation: AttributeModifier.Operation): AttributeModifier {
        return try {
            // 尝试新版构造函数 (NamespacedKey, double, Operation, EquipmentSlotGroup)
            val namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey")
            val equipmentSlotGroupClass = Class.forName("org.bukkit.inventory.EquipmentSlotGroup")
            val anySlot = equipmentSlotGroupClass.getField("ANY").get(null)
            val key = namespacedKeyClass.getConstructor(String::class.java, String::class.java)
                .newInstance("orryx", name.lowercase().replace("@", "_"))
            AttributeModifier::class.java.getConstructor(
                namespacedKeyClass,
                Double::class.javaPrimitiveType,
                AttributeModifier.Operation::class.java,
                equipmentSlotGroupClass
            ).newInstance(key, amount, operation, anySlot)
        } catch (_: Exception) {
            // 回退到旧版构造函数
            @Suppress("DEPRECATION")
            AttributeModifier(name, amount, operation)
        }
    }

    /**
     * 通过名称匹配 AttributeModifier（兼容新旧版本）
     */
    fun matchesModifierName(modifier: AttributeModifier, name: String): Boolean {
        return try {
            // 新版本使用 getKey().getKey()
            val key = modifier.javaClass.getMethod("getKey").invoke(modifier)
            val keyName = key.javaClass.getMethod("getKey").invoke(key) as String
            keyName == name.lowercase().replace("@", "_")
        } catch (_: Exception) {
            // 旧版本使用 getName()
            @Suppress("DEPRECATION")
            modifier.name == name
        }
    }
}
