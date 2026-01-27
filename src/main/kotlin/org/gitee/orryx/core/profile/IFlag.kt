package org.gitee.orryx.core.profile

import org.bukkit.entity.Player

/**
 * 玩家/全局 Flag 的基础结构。
 *
 * 持久化说明：当 [isPersistence] 为 true 时，[value] 必须是可序列化类型，否则会抛出“无法序列化”异常。
 *
 * 可持久化类型（及其包装类型）：
 * - `String`、`Boolean`、`Int`、`Long`、`Float`、`Double`、`Short`、`Byte`、`Char`
 * - `java.util.Date`、`java.time.Instant`
 * - `org.joml.Vector3dc`、`org.joml.Matrix3dc`
 * - `java.util.UUID`
 * - `org.bukkit.inventory.ItemStack`
 * - `AbstractBukkitEntity`（持久化为实体 UUID）
 * - `AbstractAdyeshachEntity`（持久化为实体 ID）
 * - `Array<*>`（数组元素必须同样是上述可序列化类型且不可为 null）
 *
 * 注意：
 * - 实体类型反序列化依赖实体是否仍存在，若实体不存在可能会返回 null 的 Flag。
 * - ItemStack 使用 TabooLib 的二进制序列化（`serializeToByteArray(true)` / `deserializeToItemStack(true)`），
 *   兼容性取决于服务端与 TabooLib 的实现。
 * - Array 类型反序列化后得到的是元素列表，使用时请按实际类型处理。
 *
 * @property value Flag 的值
 * @property isPersistence 是否持久化
 * @property timestamp 出生时间戳（毫秒）
 * @property timeout 存活时间（毫秒），`0` 为永久
 */
sealed interface IFlag {

    val value: Any

    val isPersistence: Boolean

    val timestamp: Long

    val timeout: Long

    /**
     * 是否超时。
     *
     * @return 当 [timeout] 不为 0 且超出存活时间时返回 true
     */
    fun isTimeout(): Boolean {
        return  timeout != 0L && timeout + timestamp < System.currentTimeMillis()
    }

    /**
     * 初始化 Flag 的生命周期逻辑。
     *
     * @param player 关联的玩家
     * @param key Flag 的标识
     */
    fun init(player: Player, key: String)

    /**
     * 取消 Flag 的生命周期逻辑。
     *
     * @param player 关联的玩家
     * @param key Flag 的标识
     */
    fun cancel(player: Player, key: String)
}
