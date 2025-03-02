package org.gitee.orryx.dao.cache

import com.github.benmanes.caffeine.cache.stats.CacheStats
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.dao.pojo.PlayerData
import org.gitee.orryx.dao.pojo.PlayerJob
import org.gitee.orryx.dao.pojo.PlayerSkill
import org.gitee.orryx.utils.RedisChannelPlugin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common5.format
import taboolib.module.chat.colored
import java.util.*

interface ICacheManager {

    companion object {

        private val type
            get() = OrryxAPI.config.getString("CacheManager", "memory")!!.uppercase()

        private val lazy by lazy { type }

        internal lateinit var INSTANCE: ICacheManager

        @Awake(LifeCycle.ENABLE)
        private fun loadCache() {
            INSTANCE = when(lazy) {
                "MEMORY", "CODE" -> {
                    info(("&e┣&7已选择代码内部缓存 &a√").colored())
                    MemoryManager()
                }
                "REDIS" -> {
                    if (RedisChannelPlugin.isEnabled) {
                        info(("&e┣&7已选择Redis缓存 &a√").colored())
                        RedisManager()
                    } else {
                        info(("&e┣&7因为未检测到RedisChannel，已自动选择代码内部缓存 &a√").colored())
                        MemoryManager()
                    }
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

        @Awake(LifeCycle.DISABLE)
        private fun disable() {
            val instance = INSTANCE as? MemoryManager ?: return
            fun printStats(name: String, stats: CacheStats) {
                info("&e┣&f缓存：$name &c命中率：${(stats.hitRate()*100).format(2)} % &c加载平均时间：${stats.averageLoadPenalty()/1000000} ms".colored())
            }
            printStats("玩家", instance.playerDataCache.stats())
            printStats("职业", instance.playerJobCache.stats())
            printStats("技能", instance.playerSkillCache.stats())
        }

    }

    /**
     * 从缓存获取玩家数据
     *
     * 如果数据不存在将自动从storage中调用并保存到缓存
     * @param player 玩家的UUID
     * @return 玩家数据
     * */
    fun getPlayerData(player: UUID): PlayerData?

    /**
     * 从缓存获取职业数据
     *
     * 如果数据不存在将自动从storage中调用并保存到缓存
     * @param player 玩家的UUID
     * @param job 获取的职业
     * @return 职业数据
     * */
    fun getPlayerJob(player: UUID, job: String): PlayerJob?

    /**
     * 从缓存获取技能数据
     *
     * 如果数据不存在将自动从storage中调用并保存到缓存
     * @param player 玩家的UUID
     * @param job 获取的职业
     * @param skill 获取的技能
     * @return 技能数据
     * */
    fun getPlayerSkill(player: UUID, job: String, skill: String): PlayerSkill?

    /**
     * 保存玩家数据到缓存
     * @param player 玩家的UUID
     * @param playerData 玩家数据
     * @param async 是否异步
     * */
    fun savePlayerData(player: UUID, playerData: PlayerData, async: Boolean)

    /**
     * 保存职业数据到缓存
     * @param player 玩家的UUID
     * @param playerJob 职业数据
     * @param async 是否异步
     * */
    fun savePlayerJob(player: UUID, playerJob: PlayerJob, async: Boolean)

    /**
     * 保存技能数据到缓存
     * @param player 玩家的UUID
     * @param playerSkill 技能数据
     * @param async 是否异步
     * */
    fun savePlayerSkill(player: UUID, playerSkill: PlayerSkill, async: Boolean)

    /**
     * 删除缓存中的玩家数据
     * @param player 玩家的UUID
     * @param async 是否异步
     * */
    fun removePlayerData(player: UUID, async: Boolean)

    /**
     * 删除缓存中的职业数据
     * @param player 玩家的UUID
     * @param job 职业键名
     * @param async 是否异步
     * */
    fun removePlayerJob(player: UUID, job: String, async: Boolean)

    /**
     * 删除缓存中的技能数据
     * @param player 玩家的UUID
     * @param job 职业键名
     * @param skill 技能键名
     * @param async 是否异步
     * */
    fun removePlayerSkill(player: UUID, job: String, skill: String, async: Boolean)

}