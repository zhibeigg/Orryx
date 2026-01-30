package org.gitee.orryx.dao.cache

import com.gitee.redischannel.RedisChannelPlugin
import com.gitee.redischannel.RedisChannelPlugin.Type.CLUSTER
import com.gitee.redischannel.RedisChannelPlugin.Type.SINGLE
import com.gitee.redischannel.api.events.ClientStartEvent
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.utils.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.unsafeLazy
import taboolib.module.chat.colored
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 同步缓存管理接口。
 *
 * 用于跨服缓存读取与回写玩家相关数据。
 */
interface ISyncCacheManager {

    companion object {

        private val type
            get() = Orryx.config.getString("CacheManager", "DISABLE")!!.uppercase()

        private val lazy by unsafeLazy { type }

        internal lateinit var INSTANCE: ISyncCacheManager

        @Ghost
        @SubscribeEvent
        private fun loadCache(e: ClientStartEvent) {
            when(lazy) {
                "REDIS" -> {
                    consoleMessage(("&e┣&7已选择 Redis 缓存 &a√").colored())
                    INSTANCE = when (RedisChannelPlugin.type) {
                        CLUSTER -> ClusterRedisManager()
                        SINGLE -> RedisManager()
                        null -> error("Redis 暴死了")
                    }
                }
                "BROKER" -> {}
                "DISABLE" -> {}
                else -> error("未知的缓存数据库类型: $lazy")
            }
        }

        @Awake(LifeCycle.ENABLE)
        private fun enable() {
            when(lazy) {
                "REDIS" -> {
                    // 兜底：在 Redis 客户端就绪前使用本地存储，避免 INSTANCE 未初始化导致异常
                    INSTANCE = DisableManager()
                    consoleMessage(("&e┣&7Redis 客户端未就绪，临时使用本地缓存 &6*").colored())
                }
                "BROKER" -> {
                    consoleMessage(("&e┣&7已选择 BROKE 通道缓存 &a√").colored())
                    error("暂不支持")
                    //BrokerManager()
                }
                "DISABLE" -> {
                    consoleMessage(("&e┣&7已关闭跨服同步缓存 &a√").colored())
                    INSTANCE = DisableManager()
                }
                else -> error("未知的缓存数据库类型: $lazy")
            }
        }

        @Reload(1)
        private fun load() {
            if (type != lazy) {
                consoleMessage(("&e┣&6请勿在运行时修改缓存选择 &4×").colored())
            }
        }
    }

    /**
     * 从缓存获取玩家数据
     *
     * 如果数据不存在将自动从storage中调用并保存到缓存
     * @param player 玩家的UUID
     * @return 玩家数据
     * */
    fun getPlayerProfile(player: UUID): CompletableFuture<PlayerProfilePO>

    /**
     * 从缓存获取职业数据
     *
     * 如果数据不存在将自动从storage中调用并保存到缓存
     * @param player 玩家的UUID
     * @param id 玩家的ID
     * @param job 获取的职业
     * @return 职业数据
     * */
    fun getPlayerJob(player: UUID, id: Int, job: String): CompletableFuture<PlayerJobPO?>

    /**
     * 从缓存获取技能数据
     *
     * 如果数据不存在将自动从storage中调用并保存到缓存
     * @param player 玩家的UUID
     * @param id 玩家的ID
     * @param job 获取的职业
     * @param skill 获取的技能
     * @return 技能数据
     * */
    fun getPlayerSkill(player: UUID, id: Int, job: String, skill: String): CompletableFuture<PlayerSkillPO?>

    /**
     * 从缓存获取按键数据
     *
     * 如果数据不存在将自动从storage中调用并保存到缓存
     * @param player 玩家的UUID
     * @param id 玩家的ID
     * @return 按键数据
     * */
    fun getPlayerKeySetting(player: UUID, id: Int): CompletableFuture<PlayerKeySettingPO?>

    /**
     * 保存玩家数据到缓存
     * @param player 玩家的UUID
     * @param playerProfilePO 玩家数据
     * */
    fun savePlayerProfile(player: UUID, playerProfilePO: PlayerProfilePO)

    /**
     * 保存职业数据到缓存
     * @param player 玩家的UUID
     * @param playerJobPO 职业数据
     * */
    fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO)

    /**
     * 保存技能数据到缓存
     * @param player 玩家的UUID
     * @param playerSkillPO 技能数据
     * */
    fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO)

    /**
     * 保存按键数据到缓存
     * @param player 玩家的UUID
     * @param playerKeySettingPO 按键数据
     * */
    fun savePlayerKeySetting(player: UUID, playerKeySettingPO: PlayerKeySettingPO)

    /**
     * 删除缓存中的玩家数据
     * @param player 玩家的UUID
     * */
    fun removePlayerProfile(player: UUID)

    /**
     * 删除缓存中的职业数据
     * @param player 玩家的UUID
     * @param job 职业键名
     * */
    fun removePlayerJob(player: UUID, id: Int, job: String)

    /**
     * 删除缓存中的技能数据
     * @param player 玩家的UUID
     * @param job 职业键名
     * @param skill 技能键名
     * */
    fun removePlayerSkill(player: UUID, id: Int, job: String, skill: String)

    /**
     * 删除缓存中的按键数据
     * @param player 玩家的UUID
     * */
    fun removePlayerKeySetting(player: UUID)

}
