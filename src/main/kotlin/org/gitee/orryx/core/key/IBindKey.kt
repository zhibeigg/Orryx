package org.gitee.orryx.core.key

import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.IKeyRegister

/**
 * 按键绑定接口。
 *
 * @property key 绑定键名
 * @property sort 排序权重
 * @property category ArcartX客户端按键分类（为null则非客户端按键）
 * @property defaultKey ArcartX客户端按键默认键位（为null则非客户端按键）
 */
interface IBindKey {

    val key: String

    val sort: Int

    val category: String?
        get() = null

    val defaultKey: String?
        get() = null

    /**
     * 是否为ArcartX自定义客户端按键
     */
    val isClientKeyBind: Boolean
        get() = category != null && defaultKey != null

    /**
     * 校验并执行按键触发。
     *
     * @param player 玩家
     * @param key 按键序列
     * @param timeout 超时时间（毫秒）
     * @param actionType 触发动作类型
     * @param sort 是否参与排序
     * @return 是否执行成功
     */
    fun checkAndCast(player: Player, key: List<String>, timeout: Long, actionType: IKeyRegister.ActionType, sort: Boolean): Boolean
}
