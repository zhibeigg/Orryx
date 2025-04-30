package org.gitee.orryx.module.mana

import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.ConfigLazy
import org.gitee.orryx.utils.ReloadableLazy
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.service.PlatformExecutor
import taboolib.platform.util.onlinePlayers
import java.util.concurrent.CompletableFuture

interface IManaManager {

    companion object {

        internal var INSTANCE: IManaManager = ManaMangerDefault()

        private var thread: PlatformExecutor.PlatformTask? = null

        private val regainTick: Long by ConfigLazy(Orryx.config) { Orryx.config.getLong("ManaRegainTick", 20) }

        @Reload(2)
        @Awake(LifeCycle.ENABLE)
        private fun init() {
            thread?.cancel()
            thread = submitAsync(period = regainTick) {
                onlinePlayers.forEach {
                    INSTANCE.regainMana(it)
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
    fun giveMana(player: Player, mana: Double): CompletableFuture<ManaResult>

    /**
     * 减少玩家的法力值
     * @param player 玩家
     * @param mana 法力值
     * @return 结果
     * */
    fun takeMana(player: Player, mana: Double): CompletableFuture<ManaResult>

    /**
     * 设置玩家的法力值
     * @param player 玩家
     * @param mana 法力值
     * @return 结果
     * */
    fun setMana(player: Player, mana: Double): CompletableFuture<ManaResult>

    /**
     * 玩家是否有足够的法力值
     * @param player 玩家
     * @param mana 法力值
     * @return 是否有足够法力值
     * */
    fun haveMana(player: Player, mana: Double): CompletableFuture<Boolean>

    /**
     * 获得玩家的法力值
     * @param player 玩家
     * */
    fun getMana(player: Player): CompletableFuture<Double>

    /**
     * 获得玩家的最大法力值
     * @param player 玩家
     * */
    fun getMaxMana(player: Player): CompletableFuture<Double>

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
    fun regainMana(player: Player): CompletableFuture<Double>

    /**
     * 恢复满法力值
     * @param player 玩家
     * @return 恢复的法力值
     * */
    fun healMana(player: Player): CompletableFuture<Double>
}