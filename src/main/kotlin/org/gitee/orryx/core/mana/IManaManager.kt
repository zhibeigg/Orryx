package org.gitee.orryx.core.mana

import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.reload.Reload
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.service.PlatformExecutor
import taboolib.platform.util.onlinePlayers

interface IManaManager {

    companion object {

        internal var INSTANCE: IManaManager = ManaMangerDefault()

        private var thread: PlatformExecutor.PlatformTask? = null

        private var reginTick: Long = OrryxAPI.config.getLong("ManaReginTick", 20)

        @Reload(2)
        @Awake(LifeCycle.ENABLE)
        private fun init() {
            reginTick = OrryxAPI.config.getLong("ManaReginTick", 20)
            thread?.cancel()
            thread = submitAsync(period = reginTick) {
                onlinePlayers.forEach {
                    INSTANCE.reginMana(it)
                }
            }
        }

        internal fun closeThread() {
            thread?.cancel()
        }

    }

    /**
     * 增加玩家的法力值
     * @param player 玩家
     * @param mana 法力值
     * @return 结果
     * */
    fun giveMana(player: Player, mana: Double): ManaResult

    /**
     * 减少玩家的法力值
     * @param player 玩家
     * @param mana 法力值
     * @return 结果
     * */
    fun takeMana(player: Player, mana: Double): ManaResult

    /**
     * 设置玩家的法力值
     * @param player 玩家
     * @param mana 法力值
     * @return 结果
     * */
    fun setMana(player: Player, mana: Double): ManaResult

    /**
     * 玩家是否有足够的法力值
     * @param player 玩家
     * @param mana 法力值
     * @return 是否有足够法力值
     * */
    fun haveMana(player: Player, mana: Double): Boolean

    /**
     * 获得玩家的法力值
     * @param player 玩家
     * */
    fun getMana(player: Player): Double

    /**
     * 获得玩家的最大法力值
     * @param player 玩家
     * */
    fun getMaxMana(player: Player): Double

    /**
     * 获得职业的最大法力值
     * @param sender 获取者
     * @param job 职业键名
     * @param level 等级
     * @return 法力值
     * */
    fun getMaxMana(sender: ProxyCommandSender, job: String, level: Int): Double

    /**
     * 获得职业的最大法力值
     * @param sender 获取者
     * @param job 职业
     * @param level 等级
     * @return 法力值
     * */
    fun getMaxMana(sender: ProxyCommandSender, job: IJob, level: Int): Double

    /**
     * 自然恢复法力值
     * @param player 玩家
     * @return 恢复的法力值
     * */
    fun reginMana(player: Player): Double

    /**
     * 恢复满法力值
     * @param player 玩家
     * @return 恢复的法力值
     * */
    fun healMana(player: Player): Double

}