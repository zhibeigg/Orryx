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

    val GENERIC_MAX_HEALTH: Attribute by lazy { requireAttribute("GENERIC_MAX_HEALTH", "MAX_HEALTH") }
    val GENERIC_MOVEMENT_SPEED: Attribute by lazy { requireAttribute("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED") }
    val GENERIC_KNOCKBACK_RESISTANCE: Attribute by lazy {
        requireAttribute("GENERIC_KNOCKBACK_RESISTANCE", "KNOCKBACK_RESISTANCE")
    }

    private fun requireAttribute(vararg names: String): Attribute {
        return names.firstNotNullOfOrNull(::getAttribute)
            ?: error("当前 Bukkit 版本缺少属性: ${names.joinToString()}")
    }

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
            val safeName = name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9._-]"), "_")
            val key = namespacedKeyClass.getConstructor(String::class.java, String::class.java)
                .newInstance("orryx", safeName)
            AttributeModifier::class.java.getConstructor(
                namespacedKeyClass,
                Double::class.javaPrimitiveType,
                AttributeModifier.Operation::class.java,
                equipmentSlotGroupClass
            ).newInstance(key, amount, operation, anySlot)
        } catch (_: ReflectiveOperationException) {
            createLegacyAttributeModifier(name, amount, operation)
        } catch (_: LinkageError) {
            createLegacyAttributeModifier(name, amount, operation)
        } catch (_: SecurityException) {
            createLegacyAttributeModifier(name, amount, operation)
        }
    }

    private fun createLegacyAttributeModifier(
        name: String,
        amount: Double,
        operation: AttributeModifier.Operation,
    ): AttributeModifier {
        return AttributeModifier::class.java.getConstructor(
            String::class.java,
            Double::class.javaPrimitiveType,
            AttributeModifier.Operation::class.java,
        ).newInstance(name, amount, operation)
    }

    /**
     * 通过名称匹配 AttributeModifier（兼容新旧版本）
     */
    fun matchesModifierName(modifier: AttributeModifier, name: String): Boolean {
        val safeName = name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9._-]"), "_")
        val keyName = invokeString(modifier, "getKey") { key -> invokeString(key, "getKey") }
        if (keyName != null) return keyName == safeName
        return invokeString(modifier, "getName") == name
    }

    private fun invokeString(target: Any, methodName: String, transform: ((Any) -> String?)? = null): String? {
        return try {
            val value = target.javaClass.getMethod(methodName).invoke(target) ?: return null
            transform?.invoke(value) ?: value as? String
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: LinkageError) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
}
