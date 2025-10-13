package org.gitee.orryx.dao.cache

import com.gitee.redischannel.RedisChannelPlugin
import com.gitee.redischannel.RedisChannelPlugin.Type.*
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.util.unsafeLazy
import taboolib.module.chat.colored
import java.util.*
import java.util.concurrent.CompletableFuture

interface ISyncCacheManager {

    companion object {

        private val type
            get() = Orryx.config.getString("CacheManager", "DISABLE")!!.uppercase()

        private val lazy by unsafeLazy { type }

        internal lateinit var INSTANCE: ISyncCacheManager

        @Awake(LifeCycle.ENABLE)
        private fun loadCache() {
            INSTANCE = when(lazy) {
                "REDIS" -> {
                    info(("&e┣&7已选择Redis缓存 &a√").colored())
                    when (RedisChannelPlugin.type) {
                        CLUSTER -> ClusterRedisManager()
                        SINGLE -> RedisManager()
                    }
                }
                "BROKER" -> {
                    info(("&e┣&7已选择BROKE通道缓存 &a√").colored())
                    error("暂不支持")
                    //BrokerManager()
                }
                "DISABLE" -> {
                    info(("&e┣&7已关闭跨服同步缓存 &a√").colored())
                    DisableManager()
                }
                else -> error("未知的缓存数据库类型: $lazy")
            }
        }

        @Reload(1)
        private fun load() {
            if (type != lazy) {
                info(("&e┣&6请勿在运行时修改缓存选择 &4×").colored())
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
     * @param async 是否异步
     * */
    fun savePlayerProfile(player: UUID, playerProfilePO: PlayerProfilePO)

    /**
     * 保存职业数据到缓存
     * @param player 玩家的UUID
     * @param playerJobPO 职业数据
     * @param async 是否异步
     * */
    fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO)

    /**
     * 保存技能数据到缓存
     * @param player 玩家的UUID
     * @param playerSkillPO 技能数据
     * @param async 是否异步
     * */
    fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO)

    /**
     * 保存按键数据到缓存
     * @param player 玩家的UUID
     * @param playerKeySettingPO 按键数据
     * @param async 是否异步
     * */
    fun savePlayerKeySetting(player: UUID, playerKeySettingPO: PlayerKeySettingPO)

    /**
     * 删除缓存中的玩家数据
     * @param player 玩家的UUID
     * @param async 是否异步
     * */
    fun removePlayerProfile(player: UUID)

    /**
     * 删除缓存中的职业数据
     * @param player 玩家的UUID
     * @param job 职业键名
     * @param async 是否异步
     * */
    fun removePlayerJob(player: UUID, id: Int, job: String)

    /**
     * 删除缓存中的技能数据
     * @param player 玩家的UUID
     * @param job 职业键名
     * @param skill 技能键名
     * @param async 是否异步
     * */
    fun removePlayerSkill(player: UUID, id: Int, job: String, skill: String)

    /**
     * 删除缓存中的按键数据
     * @param player 玩家的UUID
     * @param async 是否异步
     * */
    fun removePlayerKeySetting(player: UUID)

}