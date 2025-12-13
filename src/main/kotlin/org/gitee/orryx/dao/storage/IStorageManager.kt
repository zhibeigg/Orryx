package org.gitee.orryx.dao.storage

import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.utils.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.io.newFile
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.getDataFolder
import taboolib.common.util.unsafeLazy
import taboolib.module.chat.colored
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture

interface IStorageManager {

    companion object {

        private val type
            get() = Orryx.config.getString("Database.use", "SQLLITE")!!.uppercase()

        internal val file: File
            get() = newFile(
                Orryx.config.getString("Database.file", getDataFolder().absolutePath)!!.replace("{0}", getDataFolder().absolutePath),
                create = true,
                folder = true
            )

        internal val lazyType: String by unsafeLazy { type }

        internal lateinit var INSTANCE: IStorageManager

        @Awake(LifeCycle.ENABLE)
        private fun enable() {
            INSTANCE = when(lazyType) {
                "SQLLITE", "SQL_LITE" -> {
                    consoleMessage(("&e┣&7已选择SqlLite存储 &a√").colored())
                    SqlLiteManager()
                }
                "MYSQL" -> {
                    consoleMessage(("&e┣&7已选择MySql存储 &a√").colored())
                    MySqlManager()
                }
                "H2" -> {
                    consoleMessage(("&e┣&7已选择H2存储 &a√").colored())
                    H2Manager()
                }
                else -> error("未知的持久化数据库类型: $lazyType")
            }
        }

        @Reload(1)
        private fun load() {
            if (type != lazyType) {
                consoleMessage(("&e┣&6请勿在运行时修改数据库选择 &4×").colored())
            }
        }

        @SubscribeEvent
        private fun quit(e: PlayerQuitEvent) {
            (INSTANCE as? SqlLiteManager)?.quit(e.player.uniqueId)
        }
    }

    /**
     * 从数据库获取玩家数据
     * @param player 玩家的UUID
     * @return 玩家数据
     * */
    fun getPlayerData(player: UUID): CompletableFuture<PlayerProfilePO>

    /**
     * 从数据库获取职业数据
     * @param player 玩家的UUID
     * @param id 玩家的ID
     * @param job 获取的职业
     * @return 职业数据
     * */
    fun getPlayerJob(player: UUID, id: Int, job: String): CompletableFuture<PlayerJobPO?>

    /**
     * 从数据库获取技能数据
     * @param player 玩家的UUID
     * @param id 玩家的ID
     * @param job 获取的职业
     * @param skill 获取的技能
     * @return 技能数据
     * */
    fun getPlayerSkill(player: UUID, id: Int, job: String, skill: String): CompletableFuture<PlayerSkillPO?>

    /**
     * 从数据库获取技能数据列表
     * @param player 玩家的UUID
     * @param id 玩家的ID
     * @param job 获取的职业
     * @return 技能数据列表
     * */
    fun getPlayerSkills(player: UUID, id: Int, job: String): CompletableFuture<List<PlayerSkillPO>>

    /**
     * 从数据库获取按键数据列表
     * @param id 玩家的ID
     * @return 按键数据列表
     * */
    fun getPlayerKey(id: Int): CompletableFuture<PlayerKeySettingPO?>

    /**
     * 保存玩家数据到数据库
     * @param playerProfilePO 玩家数据
     * @param onSuccess 成功时执行
     * */
    fun savePlayerData(playerProfilePO: PlayerProfilePO, onSuccess: Runnable)

    /**
     * 保存职业数据到数据库
     * @param playerJobPO 职业数据
     * @param onSuccess 成功时执行
     * */
    fun savePlayerJob(playerJobPO: PlayerJobPO, onSuccess: Runnable)

    /**
     * 保存技能数据到数据库
     * @param playerSkillPO 技能数据
     * @param onSuccess 成功时执行
     * */
    fun savePlayerSkill(playerSkillPO: PlayerSkillPO, onSuccess: Runnable)

    /**
     * 保存按键数据到数据库
     * @param playerKeySettingPO 按键数据
     * @param onSuccess 成功时执行
     * */
    fun savePlayerKey(playerKeySettingPO: PlayerKeySettingPO, onSuccess: Runnable)

    /**
     * 获取全局 Flag
     * @param key flag key
     * @return [IFlag]
     * */
    fun getGlobalFlag(key: String): CompletableFuture<IFlag?>

    /**
     * 保存全局 Flag
     * @param key flag key
     * @param flag flag 为空时删除
     * @param onSuccess 成功时执行
     * */
    fun saveGlobalFlag(key: String, flag: IFlag?, onSuccess: Runnable)
}