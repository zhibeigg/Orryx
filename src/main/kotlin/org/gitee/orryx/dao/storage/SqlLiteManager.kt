package org.gitee.orryx.dao.storage

import com.eatthepath.uuid.FastUUID
import com.google.common.collect.Interner
import com.google.common.collect.Interners
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.utils.*
import taboolib.common.io.newFile
import taboolib.common.platform.function.isPrimaryThread
import taboolib.module.database.ColumnOptionSQLite
import taboolib.module.database.ColumnTypeSQLite
import taboolib.module.database.Table
import taboolib.module.database.getHost
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.measureTime

@Suppress("DuplicatedCode")
class SqlLiteManager: IStorageManager {

    private val host = newFile(IStorageManager.file, "data.db").getHost()
    private val dataSource = host.createDataSource()

    private val pluginInterner = Interners.newStrongInterner<String>()
    private val internerMap = ConcurrentHashMap<UUID, Interner<String>>()

    fun getInternerByUUID(uuid: UUID): Interner<String> = internerMap.getOrPut(uuid) { Interners.newStrongInterner() }

    private val playerTable: Table<*, *> = Table("orryx_player", host) {
        add { id() }
        add(PLAYER_UUID) { type(ColumnTypeSQLite.TEXT) { options(ColumnOptionSQLite.UNIQUE, ColumnOptionSQLite.NOTNULL) } }
        add(JOB) { type(ColumnTypeSQLite.TEXT) }
        add(POINT) { type(ColumnTypeSQLite.INTEGER) }
        add(FLAGS) { type(ColumnTypeSQLite.TEXT) }
    }

    private val jobsTable: Table<*, *> = Table("orryx_player_jobs", host) {
        add(USER_ID) {
            type(ColumnTypeSQLite.INTEGER)
            { options(ColumnOptionSQLite.NOTNULL) }
        }
        add(JOB) { type(ColumnTypeSQLite.TEXT) }
        add(EXPERIENCE) { type(ColumnTypeSQLite.INTEGER) }
        add(GROUP) { type(ColumnTypeSQLite.TEXT) }
        add(BIND_KEY_OF_GROUP) { type(ColumnTypeSQLite.TEXT) }
    }

    private val skillsTable: Table<*, *> = Table("orryx_player_job_skills", host) {
        add(USER_ID) {
            type(ColumnTypeSQLite.INTEGER)
            { options(ColumnOptionSQLite.NOTNULL) }
        }
        add(JOB) { type(ColumnTypeSQLite.TEXT) }
        add(SKILL) { type(ColumnTypeSQLite.TEXT) }
        add(LOCKED) { type(ColumnTypeSQLite.BLOB) }
        add(LEVEL) { type(ColumnTypeSQLite.INTEGER) }
    }

    private val keyTable: Table<*, *> = Table("orryx_player_key_setting", host) {
        add(USER_ID) {
            type(ColumnTypeSQLite.INTEGER)
            { options(ColumnOptionSQLite.NOTNULL, ColumnOptionSQLite.PRIMARY_KEY) }
        }
        add(KEY_SETTING) { type(ColumnTypeSQLite.TEXT) }
    }

    private val globalFlagTable: Table<*, *> = Table("orryx_global_flag", host) {
        add(FLAG_KEY) {
            type(ColumnTypeSQLite.TEXT)
            { options(ColumnOptionSQLite.NOTNULL, ColumnOptionSQLite.UNIQUE) }
        }
        add(FLAG) { type(ColumnTypeSQLite.TEXT) }
    }

    init {
        playerTable.createTable(dataSource)
        jobsTable.createTable(dataSource)
        skillsTable.createTable(dataSource)
        keyTable.createTable(dataSource)
        globalFlagTable.createTable(dataSource)
    }

    /**
     * 耗时统计器
     */
    private class TimeStats {
        private var totalTime: Long = 0
        private var count: Long = 0

        @Synchronized
        fun record(millis: Long): Pair<Long, Long> {
            totalTime += millis
            count++
            return millis to (totalTime / count)
        }
    }

    private val statsMap = mutableMapOf<String, TimeStats>()

    private fun getStats(key: String): TimeStats = statsMap.getOrPut(key) { TimeStats() }

    /**
     * 统一的异步读取模板
     */
    private inline fun <T> asyncRead(debugMessage: String, crossinline block: () -> T): CompletableFuture<T> {
        debug("SqlLite $debugMessage")
        val future = CompletableFuture<T>()
        val execute = {
            requireAsync("sqlLite")
            try {
                val result: T
                val time = measureTime { result = block() }.inWholeMilliseconds
                val (current, avg) = getStats(debugMessage).record(time)
                debug("&f$debugMessage &7| &e${current}ms &7| &f平均 &e${avg}ms")
                future.complete(result)
            } catch (e: Throwable) {
                e.printStackTrace()
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            OrryxAPI.ioScope.launch { execute() }
        } else {
            execute()
        }
        return future
    }

    override fun getPlayerData(player: UUID): CompletableFuture<PlayerProfilePO> = asyncRead("获取玩家 Profile") {
        val uuid = FastUUID.toString(player)
        synchronized(getInternerByUUID(player).intern("PlayerData$uuid")) {
            if (!playerTable.find(dataSource) { where { PLAYER_UUID eq uuid } }) {
                playerTable.insert(dataSource, PLAYER_UUID, JOB, POINT, FLAGS) {
                    value(uuid, null, 0, null)
                }
            }
        }
        playerTable.select(dataSource) {
            where { PLAYER_UUID eq uuid }
            rows(USER_ID, JOB, POINT, FLAGS)
            limit(1)
        }.first {
            PlayerProfilePO(
                getInt(USER_ID),
                player,
                getString(JOB),
                getInt(POINT),
                getString(FLAGS)?.let { Json.decodeFromString(it) } ?: emptyMap()
            )
        }
    }

    override fun getPlayerJob(player: UUID, id: Int, job: String): CompletableFuture<PlayerJobPO?> = asyncRead("获取玩家 Job") {
        jobsTable.select(dataSource) {
            where { USER_ID eq id and (JOB eq job) }
            rows(EXPERIENCE, GROUP, BIND_KEY_OF_GROUP)
            limit(1)
        }.firstOrNull {
            PlayerJobPO(
                id,
                player,
                job,
                getInt(EXPERIENCE),
                getString(GROUP),
                Json.decodeFromString(getString(BIND_KEY_OF_GROUP))
            )
        }
    }

    override fun getPlayerSkill(player: UUID, id: Int, job: String, skill: String): CompletableFuture<PlayerSkillPO?> = asyncRead("获取玩家 Skill") {
        skillsTable.select(dataSource) {
            where { USER_ID eq id and (JOB eq job) and (SKILL eq skill) }
            rows(LOCKED, LEVEL)
            limit(1)
        }.firstOrNull {
            PlayerSkillPO(id, player, job, skill, getBoolean(LOCKED), getInt(LEVEL))
        }
    }

    override fun getPlayerSkills(player: UUID, id: Int, job: String): CompletableFuture<List<PlayerSkillPO>> = asyncRead("获取玩家 Skills") {
        skillsTable.select(dataSource) {
            where { USER_ID eq id and (JOB eq job) }
            rows(SKILL, LOCKED, LEVEL)
        }.map {
            PlayerSkillPO(id, player, job, getString(SKILL), getBoolean(LOCKED), getInt(LEVEL))
        }
    }

    override fun getPlayerKey(id: Int): CompletableFuture<PlayerKeySettingPO?> = asyncRead("获取玩家 KeySetting") {
        keyTable.select(dataSource) {
            where { USER_ID eq id }
            rows(KEY_SETTING)
            limit(1)
        }.firstOrNull {
            Json.decodeFromString<PlayerKeySettingPO>(getString(KEY_SETTING))
        }
    }

    override fun savePlayerData(playerProfilePO: PlayerProfilePO, onSuccess: () -> Unit) {
        requireAsync("sqlLite")
        debug("SqlLite 保存玩家 Profile")
        playerTable.update(dataSource) {
            where { USER_ID eq playerProfilePO.id }
            set(JOB, playerProfilePO.job)
            set(POINT, playerProfilePO.point)
            set(FLAGS, Json.encodeToString(playerProfilePO.flags))
        }
        onSuccess()
    }

    override fun savePlayerJob(playerJobPO: PlayerJobPO, onSuccess: () -> Unit) {
        requireAsync("sqlLite")
        debug("SqlLite 保存玩家 Job")
        synchronized(getInternerByUUID(playerJobPO.player).intern("PlayerJob${playerJobPO.id}${playerJobPO.job}")) {
            jobsTable.workspace(dataSource) {
                if (select { where { USER_ID eq playerJobPO.id and (JOB eq playerJobPO.job) } }.find()) {
                    update {
                        where { USER_ID eq playerJobPO.id and (JOB eq playerJobPO.job) }
                        set(EXPERIENCE, playerJobPO.experience)
                        set(GROUP, playerJobPO.group)
                        set(BIND_KEY_OF_GROUP, Json.encodeToString(playerJobPO.bindKeyOfGroup))
                    }
                } else {
                    insert(USER_ID, JOB, EXPERIENCE, GROUP, BIND_KEY_OF_GROUP) {
                        value(
                            playerJobPO.id,
                            playerJobPO.job,
                            playerJobPO.experience,
                            playerJobPO.group,
                            Json.encodeToString(playerJobPO.bindKeyOfGroup)
                        )
                    }
                }
            }.run()
            onSuccess()
        }
    }

    override fun savePlayerSkill(playerSkillPO: PlayerSkillPO, onSuccess: () -> Unit) {
        requireAsync("sqlLite")
        debug("SqlLite 保存玩家 Skill")
        synchronized(getInternerByUUID(playerSkillPO.player).intern("PlayerSkill${playerSkillPO.id}${playerSkillPO.job}${playerSkillPO.skill}")) {
            skillsTable.workspace(dataSource) {
                if (select { where { USER_ID eq playerSkillPO.id and (JOB eq playerSkillPO.job) and (SKILL eq playerSkillPO.skill) } }.find()) {
                    update {
                        where { USER_ID eq playerSkillPO.id and (JOB eq playerSkillPO.job) and (SKILL eq playerSkillPO.skill) }
                        set(LOCKED, playerSkillPO.locked)
                        set(LEVEL, playerSkillPO.level)
                    }
                } else {
                    insert(USER_ID, JOB, SKILL, LOCKED, LEVEL) {
                        value(
                            playerSkillPO.id,
                            playerSkillPO.job,
                            playerSkillPO.skill,
                            playerSkillPO.locked,
                            playerSkillPO.level
                        )
                    }
                }
            }.run()
            onSuccess()
        }
    }

    override fun savePlayerKey(playerKeySettingPO: PlayerKeySettingPO, onSuccess: () -> Unit) {
        requireAsync("sqlLite")
        debug("SqlLite 保存玩家 KeySetting")
        synchronized(getInternerByUUID(playerKeySettingPO.player).intern("KeySetting${playerKeySettingPO.id}")) {
            keyTable.workspace(dataSource) {
                if (select { where { USER_ID eq playerKeySettingPO.id } }.find()) {
                    update {
                        where { USER_ID eq playerKeySettingPO.id }
                        set(KEY_SETTING, Json.encodeToString(playerKeySettingPO))
                    }
                } else {
                    insert(USER_ID, KEY_SETTING) {
                        value(
                            playerKeySettingPO.id,
                            Json.encodeToString(playerKeySettingPO)
                        )
                    }
                }
            }.run()
            onSuccess()
        }
    }

    fun quit(player: UUID) {
        internerMap.remove(player)
    }

    override fun getGlobalFlag(key: String): CompletableFuture<IFlag?> = asyncRead("获取全局 Flag") {
        globalFlagTable.select(dataSource) {
            where { FLAG_KEY eq key }
            rows(FLAG)
            limit(1)
        }.firstOrNull {
            Json.decodeFromString<IFlag>(getString(FLAG))
        }
    }

    override fun saveGlobalFlag(key: String, flag: IFlag?, onSuccess: () -> Unit) {
        requireAsync("sqlLite")
        debug("SqlLite 保存全局 Flag")
        synchronized(pluginInterner.intern("GlobalFlag$key")) {
            globalFlagTable.workspace(dataSource) {
                if (flag == null) {
                    delete { where { FLAG_KEY eq key } }
                } else {
                    if (select { where { FLAG_KEY eq key } }.find()) {
                        update {
                            where { FLAG_KEY eq key }
                            set(FLAG, Json.encodeToString(flag))
                        }
                    } else {
                        insert(FLAG_KEY, FLAG) {
                            value(key, Json.encodeToString(flag))
                        }
                    }
                }
            }.run()
            onSuccess()
        }
    }
}