package org.gitee.orryx.dao.storage

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

interface IStorageManager {

    companion object {

        private val type
            get() = Orryx.config.getString("Database.use", "SQLLITE")!!.uppercase()

        private val lazy: String by unsafeLazy { type }

        internal lateinit var INSTANCE: IStorageManager

        @Awake(LifeCycle.ENABLE)
        private fun enable() {
            INSTANCE = when(lazy) {
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
    fun getPlayerData(player: UUID): CompletableFuture<PlayerProfilePO?>

    /**
     * 从数据库获取职业数据
     * @param player 玩家的UUID
     * @param job 获取的职业
     * @return 职业数据
     * */
    fun getPlayerJob(player: UUID, job: String): CompletableFuture<PlayerJobPO?>

    /**
     * 从数据库获取技能数据
     * @param player 玩家的UUID
     * @param job 获取的职业
     * @param skill 获取的技能
     * @return 技能数据
     * */
    fun getPlayerSkill(player: UUID, job: String, skill: String): CompletableFuture<PlayerSkillPO?>

    /**
     * 从数据库获取技能数据列表
     * @param player 玩家的UUID
     * @param job 获取的职业
     * @return 技能数据列表
     * */
    fun getPlayerSkills(player: UUID, job: String): CompletableFuture<List<PlayerSkillPO>>

    /**
     * 从数据库获取按键数据列表
     * @param player 玩家的UUID
     * @return 按键数据列表
     * */
    fun getPlayerKey(player: UUID): CompletableFuture<PlayerKeySettingPO>

    /**
     * 保存玩家数据到数据库
     * @param player 玩家的UUID
     * @param playerProfilePO 玩家数据
     * @param onSuccess 成功时执行
     * */
    fun savePlayerData(player: UUID, playerProfilePO: PlayerProfilePO, onSuccess: () -> Unit)

    /**
     * 保存职业数据到数据库
     * @param player 玩家的UUID
     * @param playerJobPO 职业数据
     * @param onSuccess 成功时执行
     * */
    fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO, onSuccess: () -> Unit)

    /**
     * 保存技能数据到数据库
     * @param player 玩家的UUID
     * @param playerSkillPO 技能数据
     * @param onSuccess 成功时执行
     * */
    fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO, onSuccess: () -> Unit)

    /**
     * 保存按键数据到数据库
     * @param player 玩家的UUID
     * @param playerKeySettingPO 按键数据
     * @param onSuccess 成功时执行
     * */
    fun savePlayerKey(player: UUID, playerKeySettingPO: PlayerKeySettingPO, onSuccess: () -> Unit)

}