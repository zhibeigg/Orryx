package org.gitee.orryx.core.key

import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.IKeyRegister

/**
 * 按键绑定接口。
 *
 * @property key 绑定键名
 * @property sort 排序权重
 */
interface IBindKey {

    val key: String

    val sort: Int

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
