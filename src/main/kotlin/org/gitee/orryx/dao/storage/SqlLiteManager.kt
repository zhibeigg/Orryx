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

    override fun getPlayerData(player: UUID): CompletableFuture<PlayerProfilePO> {
        debug("SqlLite 获取玩家 Profile")
        val future = CompletableFuture<PlayerProfilePO>()
        fun read() {
            requireAsync("sqlLite")
            try {
                val uuid = FastUUID.toString(player)
                synchronized(getInternerByUUID(player).intern("PlayerData${uuid}")) {
                    if (!playerTable.find(dataSource) { where { PLAYER_UUID eq uuid } }) {
                        playerTable.insert(dataSource, PLAYER_UUID, JOB, POINT, FLAGS) {
                            value(uuid, null, 0, null)
                        }
                    }
                }
                future.complete(
                    playerTable.select(dataSource) {
                        where { PLAYER_UUID eq FastUUID.toString(player) }
                        rows(USER_ID, JOB, POINT, FLAGS)
                    }.firstOrNull {
                        PlayerProfilePO(
                            getInt(USER_ID),
                            player,
                            getString(JOB),
                            getInt(POINT),
                            getString(FLAGS)?.let { Json.decodeFromString(it) } ?: emptyMap()
                        )
                    }
                )
            } catch (e: Throwable) {
                e.printStackTrace()
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            OrryxAPI.ioScope.launch { read() }
        } else {
            read()
        }
        return future
    }

    override fun getPlayerJob(player: UUID, id: Int, job: String): CompletableFuture<PlayerJobPO?> {
        debug("SqlLite 获取玩家 Job")
        val future = CompletableFuture<PlayerJobPO?>()
        fun read() {
            requireAsync("sqlLite")
            try {
                future.complete(jobsTable.select(dataSource) {
                    where { USER_ID eq id and (JOB eq job) }
                    rows(EXPERIENCE, GROUP, BIND_KEY_OF_GROUP)
                }.firstOrNull {
                    PlayerJobPO(
                        id,
                        player,
                        job,
                        getInt(EXPERIENCE),
                        getString(GROUP),
                        Json.decodeFromString(getString(BIND_KEY_OF_GROUP))
                    )
                })
            } catch (e: Throwable) {
                e.printStackTrace()
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            OrryxAPI.ioScope.launch { read() }
        } else {
            read()
        }
        return future
    }

    override fun getPlayerSkill(player: UUID, id: Int, job: String, skill: String): CompletableFuture<PlayerSkillPO?> {
        debug("SqlLite 获取玩家 Skill")
        val future = CompletableFuture<PlayerSkillPO?>()
        fun read() {
            requireAsync("sqlLite")
            try {
                future.complete(skillsTable.select(dataSource) {
                    where { USER_ID eq id and (JOB eq job) and (SKILL eq skill) }
                    rows(LOCKED, LEVEL)
                }.firstOrNull {
                    PlayerSkillPO(id, player, job, skill, getBoolean(LOCKED), getInt(LEVEL))
                })
            } catch (e: Throwable) {
                e.printStackTrace()
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            OrryxAPI.ioScope.launch { read() }
        } else {
            read()
        }
        return future
    }

    override fun getPlayerSkills(player: UUID, id: Int, job: String): CompletableFuture<List<PlayerSkillPO>> {
        debug("SqlLite 获取玩家 Skills")
        val future = CompletableFuture<List<PlayerSkillPO>>()
        fun read() {
            requireAsync("sqlLite")
            try {
                future.complete(skillsTable.select(dataSource) {
                    where { USER_ID eq id and (JOB eq job) }
                    rows(SKILL, LOCKED, LEVEL)
                }.map {
                    PlayerSkillPO(id, player, job, getString(SKILL), getBoolean(LOCKED), getInt(LEVEL))
                })
            } catch (e: Throwable) {
                e.printStackTrace()
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            OrryxAPI.ioScope.launch { read() }
        } else {
            read()
        }
        return future
    }

    override fun getPlayerKey(id: Int): CompletableFuture<PlayerKeySettingPO?> {
        debug("SqlLite 获取玩家 KeySetting")
        val future = CompletableFuture<PlayerKeySettingPO?>()
        fun read() {
            requireAsync("sqlLite")
            try {
                future.complete(keyTable.select(dataSource) {
                    where { USER_ID eq id }
                    rows(KEY_SETTING)
                }.firstOrNull {
                    Json.decodeFromString<PlayerKeySettingPO>(getString(KEY_SETTING))
                })
            } catch (e: Throwable) {
                e.printStackTrace()
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            OrryxAPI.ioScope.launch { read() }
        } else {
            read()
        }
        return future
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

    override fun getGlobalFlag(key: String): CompletableFuture<IFlag?> {
        debug("SqlLite 获取全局 Flag")
        val future = CompletableFuture<IFlag?>()
        fun read() {
            requireAsync("sqlLite")
            try {
                future.complete(globalFlagTable.select(dataSource) {
                    where { FLAG_KEY eq key }
                    rows(FLAG)
                }.firstOrNull {
                    Json.decodeFromString<IFlag>(getString(FLAG))
                })
            } catch (e: Throwable) {
                e.printStackTrace()
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            OrryxAPI.ioScope.launch { read() }
        } else {
            read()
        }
        return future
    }

    override fun saveGlobalFlag(key: String, flag: IFlag?, onSuccess: () -> Unit) {
        requireAsync("sqlLite")
        debug("SqlLite 保存全局 Flag")
        synchronized(pluginInterner.intern("GlobalFlag${key}")) {
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
                            value(
                                key,
                                Json.encodeToString(flag)
                            )
                        }
                    }
                }
            }.run()
            onSuccess()
        }
    }
}