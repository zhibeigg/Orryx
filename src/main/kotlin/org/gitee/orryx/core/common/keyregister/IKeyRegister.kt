package org.gitee.orryx.core.common.keyregister

import org.bukkit.entity.Player

interface IKeyRegister {

    val player: Player

    /**
     * 获取此按键是否被按下
     * @param key 按键
     * @return 是否按下
     * */
    fun isKeyPress(key: String): Boolean

    /**
     * 获取此按键是否被释放
     * @param key 按键
     * @return 是否释放
     * */
    fun isKeyRelease(key: String): Boolean

    /**
     * 获取此按键最后一次按下的时间戳
     * @param key 按键
     * @return 最后一次按下的时间戳
     * */
    fun getKeyPressLast(key: String): Long

    /**
     * 获取此按键最后一次释放的时间戳
     * @param key 按键
     * @return 最后一次释放的时间戳
     * */
    fun getKeyReleaseLast(key: String): Long

    /**
     * 此按键最后一次 [actionType] 的时间到 [System.currentTimeMillis] 是否在 [timeout] (毫秒)内
     * @param key 按键
     * @param timeout 毫秒
     * @param actionType 动作类型
     * @see System.currentTimeMillis
     * @return 是否在时间内
     * */
    fun isKeyInTimeout(key: String, timeout: Long, actionType: ActionType = ActionType.RELEASE): Boolean

    /**
     * 此按键最后一次 [actionType] 的时间到 [timeStamp] 是否在 [timeout] (毫秒)内
     * @param key 按键
     * @param timeStamp 时间戳
     * @param timeout 毫秒
     * @param actionType 动作类型
     * @return 是否在时间内
     * */
    fun isKeyInTimeout(key: String, timeStamp: Long, timeout: Long, actionType: ActionType): Boolean

    /**
     * 此按键组是否在离最后一次[actionType]的时间[timeout](毫秒)内
     * @param keys 按键组
     * @param timeout 毫秒
     * @param actionType 动作类型
     * @param sort 是否按照顺序检测
     * @return 是否在时间内
     * */
    fun isKeysInTimeout(keys: List<String>, timeout: Long, actionType: ActionType = ActionType.RELEASE, sort: Boolean = false): Boolean

    /**
     * 按键按下注入
     * @param key 按键
     * */
    fun keyPress(key: String)

    /**
     * 按键释放注入
     * @param key 按键
     * */
    fun keyRelease(key: String)

    enum class ActionType {
        PRESS, RELEASE;
    }
}