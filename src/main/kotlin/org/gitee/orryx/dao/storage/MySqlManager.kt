package org.gitee.orryx.dao.storage

import kotlinx.serialization.json.Json
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submitAsync
import taboolib.module.database.ColumnOptionSQL
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.Table
import taboolib.module.database.getHost
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.sql.DataSource

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
    }

    init {
        playerTable.createTable(dataSource)
        jobsTable.createTable(dataSource)
        skillsTable.createTable(dataSource)
        keyTable.createTable(dataSource)
        globalFlagTable.createTable(dataSource)
    }

    override fun getPlayerData(player: UUID): CompletableFuture<PlayerProfilePO> {
        debug("${IStorageManager.lazyType} 获取玩家 Profile")
        val future = CompletableFuture<PlayerProfilePO>()
        fun read() {
            try {
                val uuid = uuidToBytes(player)
                if (!playerTable.find(dataSource) { where { PLAYER_UUID eq uuid } }) {
                    playerTable.insert(dataSource, PLAYER_UUID, JOB, POINT, FLAGS) {
                        value(uuid, null, 0, null)
                    }
                }
                future.complete(
                    playerTable.select(dataSource) {
                        where { PLAYER_UUID eq uuidToBytes(player) }
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
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            submitAsync { read() }
        } else {
            read()
        }
        return future
    }

    override fun getPlayerJob(player: UUID, id: Int, job: String): CompletableFuture<PlayerJobPO?> {
        debug("${IStorageManager.lazyType} 获取玩家 Job")
        val future = CompletableFuture<PlayerJobPO?>()
        fun read() {
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
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            submitAsync { read() }
        } else {
            read()
        }
        return future
    }

    override fun getPlayerSkill(player: UUID, id: Int, job: String, skill: String): CompletableFuture<PlayerSkillPO?> {
        debug("${IStorageManager.lazyType} 获取玩家 Skill")
        val future = CompletableFuture<PlayerSkillPO?>()
        fun read() {
            try {
                future.complete(skillsTable.select(dataSource) {
                    where { USER_ID eq id and (JOB eq job) and (SKILL eq skill) }
                    rows(LOCKED, LEVEL)
                }.firstOrNull {
                    PlayerSkillPO(id, player, job, skill, getBoolean(LOCKED), getInt(LEVEL))
                })
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            submitAsync { read() }
        } else {
            read()
        }
        return future
    }

    override fun getPlayerSkills(player: UUID, id: Int, job: String): CompletableFuture<List<PlayerSkillPO>> {
        debug("${IStorageManager.lazyType} 获取玩家 Skills")
        val future = CompletableFuture<List<PlayerSkillPO>>()
        fun read() {
            try {
                future.complete(skillsTable.select(dataSource) {
                    where { USER_ID eq id and (JOB eq job) }
                    rows(SKILL, LOCKED, LEVEL)
                }.map {
                    PlayerSkillPO(id, player, job, getString(SKILL), getBoolean(LOCKED), getInt(LEVEL))
                })
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            submitAsync { read() }
        } else {
            read()
        }
        return future
    }

    override fun getPlayerKey(id: Int): CompletableFuture<PlayerKeySettingPO?> {
        debug("${IStorageManager.lazyType} 获取玩家 KeySetting")
        val future = CompletableFuture<PlayerKeySettingPO?>()
        fun read() {
            try {
                future.complete(keyTable.select(dataSource) {
                    where { USER_ID eq id }
                    rows(KEY_SETTING)
                }.firstOrNull {
                    Json.decodeFromString<PlayerKeySettingPO>(getString(KEY_SETTING))
                })
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            submitAsync { read() }
        } else {
            read()
        }
        return future
    }

    override fun savePlayerData(playerProfilePO: PlayerProfilePO, onSuccess: () -> Unit) {
        debug("${IStorageManager.lazyType} 保存玩家 Profile")
        playerTable.update(dataSource) {
            where { USER_ID eq playerProfilePO.id }
            set(JOB, playerProfilePO.job)
            set(POINT, playerProfilePO.point)
            set(FLAGS, Json.encodeToString(playerProfilePO.flags))
        }
        onSuccess()
    }

    override fun savePlayerJob(playerJobPO: PlayerJobPO, onSuccess: () -> Unit) {
        debug("${IStorageManager.lazyType} 保存玩家 Job")
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
        }.onSuccess { onSuccess() }
    }

    override fun savePlayerSkill(playerSkillPO: PlayerSkillPO, onSuccess: () -> Unit) {
        debug("${IStorageManager.lazyType} 保存玩家 Skill")
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
        }.onSuccess { onSuccess() }
    }

    override fun savePlayerKey(playerKeySettingPO: PlayerKeySettingPO, onSuccess: () -> Unit) {
        debug("${IStorageManager.lazyType} 保存玩家 KeySetting")
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
        }.onSuccess { onSuccess() }
    }

    override fun getGlobalFlag(key: String): CompletableFuture<IFlag?> {
        debug("${IStorageManager.lazyType} 获取全局 Flag")
        val future = CompletableFuture<IFlag?>()
        fun read() {
            try {
                future.complete(globalFlagTable.select(dataSource) {
                    where { FLAG_KEY eq key }
                    rows(FLAG)
                }.firstOrNull {
                    Json.decodeFromString<IFlag>(getString(FLAG))
                })
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            submitAsync { read() }
        } else {
            read()
        }
        return future
    }

    override fun saveGlobalFlag(key: String, flag: IFlag?, onSuccess: () -> Unit) {
        debug("${IStorageManager.lazyType} 保存全局 Flag")
        globalFlagTable.transaction(dataSource) {
            if (flag == null) {
                delete { where { FLAG_KEY eq key } }
            } else {
                insert(FLAG_KEY, FLAG) {
                    onDuplicateKeyUpdate {
                        update(FLAG, Json.encodeToString(flag))
                    }
                    value(
                        key,
                        Json.encodeToString(flag)
                    )
                }
            }
        }.onSuccess { onSuccess() }
    }
}