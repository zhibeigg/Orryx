package org.gitee.orryx.module.spirit

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gitee.nodens.core.attribute.Health.Regain.period
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.ConfigLazy
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.service.PlatformExecutor
import taboolib.platform.BukkitPlugin
import taboolib.platform.util.onlinePlayers
import java.util.concurrent.CompletableFuture

interface ISpiritManager {
    
    companion object {

        internal var INSTANCE: ISpiritManager = SpiritManagerDefault()

        private var runnable: PlatformExecutor.PlatformTask? = null

        private val regainTick: Long by ConfigLazy(Orryx.config) { Orryx.config.getLong("SpiritRegainTick", 20) }
        
        @Reload(2)
        @Awake(LifeCycle.ENABLE)
        private fun init() {
            runnable?.cancel()
            runnable = submitAsync(period = regainTick) {
                onlinePlayers.forEach {
                    INSTANCE.regainSpirit(it)
                }
            }
        }

        internal fun closeThread() {
            runnable?.cancel()
        }
    }

    /**
     * 增加玩家的精力值
     * @param player 玩家
     * @param spirit 精力值
     * @return 结果
     * */
    fun giveSpirit(player: Player, spirit: Double): CompletableFuture<SpiritResult>

    /**
     * 减少玩家的精力值
     * @param player 玩家
     * @param spirit 精力值
     * @return 结果
     * */
    fun takeSpirit(player: Player, spirit: Double): CompletableFuture<SpiritResult>

    /**
     * 设置玩家的精力值
     * @param player 玩家
     * @param spirit 精力值
     * @return 结果
     * */
    fun setSpirit(player: Player, spirit: Double): CompletableFuture<SpiritResult>

    /**
     * 玩家是否有足够的精力值
     * @param player 玩家
     * @param spirit 精力值
     * @return 是否有足够精力值
     * */
    fun haveSpirit(player: Player, spirit: Double): Boolean

    /**
     * 获得玩家的精力值
     * @param player 玩家
     * */
    fun getSpirit(player: Player): Double

    /**
     * 获得玩家的最大精力值
     * @param player 玩家
     * */
    fun getMaxSpirit(player: Player): CompletableFuture<Double>

    /**
     * 获得职业的最大精力值
     * @param sender 获取者
     * @param job 职业键名
     * @param level 等级
     * @return 精力值
     * */
    fun getMaxSpirit(sender: ProxyCommandSender, job: String, level: Int): Double

    /**
     * 获得职业的最大精力值
     * @param sender 获取者
     * @param job 职业
     * @param level 等级
     * @return 精力值
     * */
    fun getMaxSpirit(sender: ProxyCommandSender, job: IJob, level: Int): Double

    /**
     * 自然恢复精力值
     * @param player 玩家
     * @return 恢复的精力值
     * */
    fun regainSpirit(player: Player): CompletableFuture<Double>

    /**
     * 恢复满精力值
     * @param player 玩家
     * @return 恢复的精力值
     * */
    fun healSpirit(player: Player): CompletableFuture<Double>
}