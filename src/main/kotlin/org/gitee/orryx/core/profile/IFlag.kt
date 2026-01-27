package org.gitee.orryx.core.profile

import org.bukkit.entity.Player

/**
 * 玩家/全局 Flag 的基础结构。
 *
 * 持久化说明：
 * - 当 [isPersistence] 为 true 时，[value] 必须是可序列化类型，否则会抛出“无法序列化”异常。
 * - 支持的持久化类型（及其包装类型）包括：
 *   String、Boolean、Int、Long、Float、Double、Short、Byte、Char
 *   java.util.Date、java.time.Instant
 *   org.joml.Vector3dc、org.joml.Matrix3dc
 *   java.util.UUID
 *   org.bukkit.inventory.ItemStack
 *   AbstractBukkitEntity（持久化为实体 UUID）
 *   AbstractAdyeshachEntity（持久化为实体 ID）
 *   Array<*>（数组元素必须同样是上述可序列化类型且不可为 null）
 *
 * 注意：
 * - 实体类型反序列化依赖实体是否仍存在，若实体不存在可能会返回 null 的 Flag。
 * - ItemStack 使用对象流序列化，跨版本兼容性取决于服务端实现。
 * - Array 类型反序列化后得到的是元素列表，使用时请按实际类型处理。
 */
sealed interface IFlag {

    /**
     * 值
     * */
    val value: Any

    /**
     * 是否持久化
     * */
    val isPersistence: Boolean

    /**
     * 出生时间戳 (毫秒)
     * */
    val timestamp: Long

    /**
     * 存活时间 (毫秒)
     *
     * 0 为永久
     * */
    val timeout: Long

    /**
     * 是否死亡
     * */
    fun isTimeout(): Boolean {
        return  timeout != 0L && timeout + timestamp < System.currentTimeMillis()
    }

    /**
     * 出生
     * */
    fun init(player: Player, key: String)

    /**
     * 取消
     * */
    fun cancel(player: Player, key: String)
}
