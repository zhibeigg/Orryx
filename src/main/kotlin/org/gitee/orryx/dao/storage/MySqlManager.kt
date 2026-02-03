package org.gitee.orryx.dao.storage

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.isPrimaryThread
import taboolib.module.database.ColumnOptionSQL
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.Table
import taboolib.module.database.getHost
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource
import kotlin.time.measureTime

@Suppress("DuplicatedCode")
class MySqlManager(replaceDataSource: DataSource? = null): IStorageManager {

    private val host by lazy { Orryx.config.getHost("Database.sql") }
    private val dataSource: DataSource = replaceDataSource ?: host.createDataSource()

    private val playerTable: Table<*, *> = Table("orryx_player", host) {
        add { id() }
        add(PLAYER_UUID) { type(ColumnTypeSQL.BINARY, 16) { options(ColumnOptionSQL.UNIQUE_KEY, ColumnOptionSQL.NOTNULL) } }
        add(JOB) { type(ColumnTypeSQL.VARCHAR, 255) }
        add(POINT) { type(ColumnTypeSQL.INT) }
        add(FLAGS) { type(ColumnTypeSQL.TEXT) }
    }

    private val jobsTable: Table<*, *> = Table("orryx_player_jobs", host) {
        add(USER_ID) {
            type(ColumnTypeSQL.BIGINT)
            { options(ColumnOptionSQL.UNSIGNED, ColumnOptionSQL.NOTNULL) }
        }
        add(JOB) { type(ColumnTypeSQL.VARCHAR, 255) { options(ColumnOptionSQL.KEY, ColumnOptionSQL.NOTNULL) } }
        add(EXPERIENCE) { type(ColumnTypeSQL.INT) }
        add(GROUP) { type(ColumnTypeSQL.VARCHAR, 255) }
        add(BIND_KEY_OF_GROUP) { type(ColumnTypeSQL.TEXT) }
        primaryKeyForLegacy.addAll(arrayOf(USER_ID, JOB))
    }

    private val skillsTable: Table<*, *> = Table("orryx_player_job_skills", host) {
        add(USER_ID) {
            type(ColumnTypeSQL.BIGINT)
            { options(ColumnOptionSQL.UNSIGNED, ColumnOptionSQL.NOTNULL) }
        }
        add(JOB) { type(ColumnTypeSQL.VARCHAR, 255) { options(ColumnOptionSQL.KEY, ColumnOptionSQL.NOTNULL) } }
        add(SKILL) { type(ColumnTypeSQL.VARCHAR, 255) { options(ColumnOptionSQL.KEY, ColumnOptionSQL.NOTNULL) } }
        add(LOCKED) { type(ColumnTypeSQL.BOOLEAN) }
        add(LEVEL) { type(ColumnTypeSQL.INT) }
        primaryKeyForLegacy.addAll(arrayOf(USER_ID, JOB, SKILL))
    }

    private val keyTable: Table<*, *> = Table("orryx_player_key_setting", host) {
        add(USER_ID) {
            type(ColumnTypeSQL.BIGINT)
            { options(ColumnOptionSQL.UNSIGNED, ColumnOptionSQL.NOTNULL, ColumnOptionSQL.PRIMARY_KEY) }
        }
        add(KEY_SETTING) { type(ColumnTypeSQL.TEXT) }
    }

    private val globalFlagTable: Table<*, *> = Table("orryx_global_flag", host) {
        add { id() }
        add(FLAG_KEY) { type(ColumnTypeSQL.VARCHAR, 255) { options(ColumnOptionSQL.UNIQUE_KEY, ColumnOptionSQL.NOTNULL) } }
        add(FLAG) { type(ColumnTypeSQL.TEXT) }
        add(DELETED) { type(ColumnTypeSQL.BOOLEAN) { def("\$FALSE") } }
    }

    init {
        runBlocking {
            listOf(
                async { playerTable.createTable(dataSource) },
                async { jobsTable.createTable(dataSource) },
                async { skillsTable.createTable(dataSource) },
                async { keyTable.createTable(dataSource) },
                async { globalFlagTable.createTable(dataSource) }
            ).awaitAll()
        }
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

    private val statsMap = ConcurrentHashMap<String, TimeStats>()

    private fun getStats(key: String): TimeStats = statsMap.getOrPut(key) { TimeStats() }

    /**
     * 统一的异步读取模板
     */
    private inline fun <T> asyncRead(debugMessage: String, crossinline block: () -> T): CompletableFuture<T> {
        debug { "${IStorageManager.lazyType} $debugMessage" }
        val future = CompletableFuture<T>()
        val execute = {
            requireAsync("mysql")
            try {
                val result: T
                val time = measureTime { result = block() }.inWholeMilliseconds
                val (current, avg) = getStats(debugMessage).record(time)
                debug { "&f$debugMessage &7| &e${current}ms &7| &f平均 &e${avg}ms" }
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
        val uuid = uuidToBytes(player)
        if (!playerTable.find(dataSource) { where { PLAYER_UUID eq uuid } }) {
            playerTable.insert(dataSource, PLAYER_UUID, JOB, POINT, FLAGS) {
                value(uuid, null, 0, null)
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

    override fun savePlayerData(playerProfilePO: PlayerProfilePO, onSuccess: Runnable) {
        requireAsync("mysql")
        debug { "${IStorageManager.lazyType} 保存玩家 Profile" }
        playerTable.update(dataSource) {
            where { USER_ID eq playerProfilePO.id }
            set(JOB, playerProfilePO.job)
            set(POINT, playerProfilePO.point)
            set(FLAGS, Json.encodeToString(playerProfilePO.flags))
        }
        onSuccess.run()
    }

    override fun savePlayerJob(playerJobPO: PlayerJobPO, onSuccess: Runnable) {
        requireAsync("mysql")
        debug { "${IStorageManager.lazyType} 保存玩家 Job" }
        jobsTable.transaction(dataSource) {
            insert(USER_ID, JOB, EXPERIENCE, GROUP, BIND_KEY_OF_GROUP) {
                onDuplicateKeyUpdate {
                    update(EXPERIENCE, playerJobPO.experience)
                    update(GROUP, playerJobPO.group)
                    update(BIND_KEY_OF_GROUP, Json.encodeToString(playerJobPO.bindKeyOfGroup))
                }
                value(
                    playerJobPO.id,
                    playerJobPO.job,
                    playerJobPO.experience,
                    playerJobPO.group,
                    Json.encodeToString(playerJobPO.bindKeyOfGroup)
                )
            }
        }.onSuccess { onSuccess.run() }
    }

    override fun savePlayerSkill(playerSkillPO: PlayerSkillPO, onSuccess: Runnable) {
        requireAsync("mysql")
        debug { "${IStorageManager.lazyType} 保存玩家 Skill" }
        skillsTable.transaction(dataSource) {
            insert(USER_ID, JOB, SKILL, LOCKED, LEVEL) {
                onDuplicateKeyUpdate {
                    update(LOCKED, playerSkillPO.locked)
                    update(LEVEL, playerSkillPO.level)
                }
                value(
                    playerSkillPO.id,
                    playerSkillPO.job,
                    playerSkillPO.skill,
                    playerSkillPO.locked,
                    playerSkillPO.level
                )
            }
        }.onSuccess { onSuccess.run() }
    }

    override fun savePlayerKey(playerKeySettingPO: PlayerKeySettingPO, onSuccess: Runnable) {
        requireAsync("mysql")
        debug { "${IStorageManager.lazyType} 保存玩家 KeySetting" }
        keyTable.transaction(dataSource) {
            insert(USER_ID, KEY_SETTING) {
                onDuplicateKeyUpdate {
                    update(KEY_SETTING, Json.encodeToString(playerKeySettingPO))
                }
                value(
                    playerKeySettingPO.id,
                    Json.encodeToString(playerKeySettingPO)
                )
            }
        }.onSuccess { onSuccess.run() }
    }

    override fun getGlobalFlag(key: String): CompletableFuture<IFlag?> = asyncRead("获取全局 Flag") {
        globalFlagTable.select(dataSource) {
            where { FLAG_KEY eq key and (DELETED eq false) }
            rows(FLAG)
            limit(1)
        }.firstOrNull {
            Json.decodeFromString<IFlag>(getString(FLAG))
        }
    }

    override fun saveGlobalFlag(key: String, flag: IFlag?, onSuccess: Runnable) {
        requireAsync("mysql")
        debug { "${IStorageManager.lazyType} 保存全局 Flag" }
        globalFlagTable.transaction(dataSource) {
            if (flag == null) {
                update {
                    where { FLAG_KEY eq key and (DELETED eq false) }
                    set(DELETED, true)
                }
            } else {
                insert(FLAG_KEY, FLAG, DELETED) {
                    onDuplicateKeyUpdate {
                        update(FLAG, Json.encodeToString(flag))
                        update(DELETED, false)
                    }
                    value(key, Json.encodeToString(flag), false)
                }
            }
        }.onSuccess { onSuccess.run() }
    }
}