package org.gitee.orryx.dao.storage

import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.dao.pojo.PlayerData
import org.gitee.orryx.dao.pojo.PlayerJob
import org.gitee.orryx.dao.pojo.PlayerSkill
import taboolib.common.platform.function.info
import taboolib.module.chat.colored
import java.util.*

interface IStorageManager {

    companion object {

        private val type
            get() = OrryxAPI.config.getString("Database.use", "SQLLITE")!!.uppercase()

        private val lazy: String by lazy { type }

        internal val INSTANCE: IStorageManager by lazy {
            when(lazy) {
                "SQLLITE", "SQL_LITE" -> {
                    info(("&e┣&7已选择SqlLite存储 &a√").colored())
                    SqlLiteManager()
                }
                "MYSQL" -> {
                    info(("&e┣&7已选择MySql存储 &a√").colored())
                    MySqlManager()
                }
                else -> error("未知的持久化数据库类型: $lazy")
            }
        }

        @Reload(1)
        private fun load() {
            if (type != lazy) {
                info(("&e┣&6请勿在运行时修改数据库选择 &4×").colored())
            }
        }

    }

    /**
     * 从数据库获取玩家数据
     * @param player 玩家的UUID
     * @return 玩家数据
     * */
    fun getPlayerData(player: UUID): PlayerData?

    /**
     * 从数据库获取职业数据
     * @param player 玩家的UUID
     * @param job 获取的职业
     * @return 职业数据
     * */
    fun getPlayerJob(player: UUID, job: String): PlayerJob?

    /**
     * 从数据库获取技能数据
     * @param player 玩家的UUID
     * @param job 获取的职业
     * @param skill 获取的技能
     * @return 技能数据
     * */
    fun getPlayerSkill(player: UUID, job: String, skill: String): PlayerSkill?

    /**
     * 保存玩家数据到数据库
     * @param player 玩家的UUID
     * @param playerData 玩家数据
     * */
    fun savePlayerData(player: UUID, playerData: PlayerData)

    /**
     * 保存职业数据到数据库
     * @param player 玩家的UUID
     * @param playerJob 职业数据
     * */
    fun savePlayerJob(player: UUID, playerJob: PlayerJob)

    /**
     * 保存技能数据到数据库
     * @param player 玩家的UUID
     * @param playerSkill 技能数据
     * */
    fun savePlayerSkill(player: UUID, playerSkill: PlayerSkill)

}