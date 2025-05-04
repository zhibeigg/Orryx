package org.gitee.orryx.dao.storage

import com.eatthepath.uuid.FastUUID
import kotlinx.serialization.json.Json
import org.gitee.orryx.api.Orryx
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

class MySqlManager: IStorageManager {

    private val host = Orryx.config.getHost("Database.sql")
    private val dataSource = host.createDataSource()

    private val playerTable: Table<*, *> = Table("orryx_player", host) {
        add(PLAYER_UUID) { type(ColumnTypeSQL.CHAR, 36) { options(ColumnOptionSQL.PRIMARY_KEY) } }
        add(JOB) { type(ColumnTypeSQL.VARCHAR, 255) }
        add(POINT) { type(ColumnTypeSQL.INT) }
        add(FLAGS) { type(ColumnTypeSQL.TEXT) }
    }

    private val jobsTable: Table<*, *> = Table("orryx_player_jobs", host) {
        add(PLAYER_UUID) { type(ColumnTypeSQL.CHAR, 36) }
        add(JOB) { type(ColumnTypeSQL.VARCHAR, 255) }
        add(EXPERIENCE) { type(ColumnTypeSQL.INT) }
        add(GROUP) { type(ColumnTypeSQL.VARCHAR, 255) }
        add(BIND_KEY_OF_GROUP) { type(ColumnTypeSQL.TEXT) }
        primaryKeyForLegacy += listOf(PLAYER_UUID, JOB)
    }

    private val skillsTable: Table<*, *> = Table("orryx_player_job_skills", host) {
        add(PLAYER_UUID) { type(ColumnTypeSQL.CHAR, 36) }
        add(JOB) { type(ColumnTypeSQL.VARCHAR, 255) }
        add(SKILL) { type(ColumnTypeSQL.VARCHAR, 255) }
        add(LOCKED) { type(ColumnTypeSQL.BOOLEAN) }
        add(LEVEL) { type(ColumnTypeSQL.INT) }
        primaryKeyForLegacy += listOf(PLAYER_UUID, JOB, SKILL)
    }

    private val keyTable: Table<*, *> = Table("orryx_player_key_setting", host) {
        add(PLAYER_UUID) { type(ColumnTypeSQL.CHAR, 36) { options(ColumnOptionSQL.PRIMARY_KEY) } }
        add(KEY_SETTING) { type(ColumnTypeSQL.TEXT) }
    }

    init {
        playerTable.createTable(dataSource)
        jobsTable.createTable(dataSource)
        skillsTable.createTable(dataSource)
        keyTable.createTable(dataSource)
    }

    override fun getPlayerData(player: UUID): CompletableFuture<PlayerProfilePO?> {
        debug("Mysql 获取玩家 Profile")
        val future = CompletableFuture<PlayerProfilePO?>()
        fun read() {
            try {
                future.complete(playerTable.select(dataSource) {
                    where { PLAYER_UUID eq FastUUID.toString(player) }
                    rows(JOB, POINT, FLAGS)
                }.firstOrNull {
                    PlayerProfilePO(
                        player,
                        getString(JOB),
                        getInt(POINT),
                        Json.decodeFromString(getString(FLAGS))
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

    override fun getPlayerJob(player: UUID, job: String): CompletableFuture<PlayerJobPO?> {
        debug("Mysql 获取玩家 Job")
        val future = CompletableFuture<PlayerJobPO?>()
        fun read() {
            try {
                future.complete(jobsTable.select(dataSource) {
                    where { PLAYER_UUID eq FastUUID.toString(player) and (JOB eq job) }
                    rows(EXPERIENCE, GROUP, BIND_KEY_OF_GROUP)
                }.firstOrNull {
                    PlayerJobPO(
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

    override fun getPlayerSkill(player: UUID, job: String, skill: String): CompletableFuture<PlayerSkillPO?> {
        debug("Mysql 获取玩家 Skill")
        val future = CompletableFuture<PlayerSkillPO?>()
        fun read() {
            try {
                future.complete(skillsTable.select(dataSource) {
                    where { PLAYER_UUID eq FastUUID.toString(player) and (JOB eq job) and (SKILL eq skill) }
                    rows(LOCKED, LEVEL)
                }.firstOrNull {
                    PlayerSkillPO(player, job, skill, getBoolean(LOCKED), getInt(LEVEL))
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

    override fun getPlayerSkills(player: UUID, job: String): CompletableFuture<List<PlayerSkillPO>> {
        debug("Mysql 获取玩家 Skills")
        val future = CompletableFuture<List<PlayerSkillPO>>()
        fun read() {
            try {
                future.complete(skillsTable.select(dataSource) {
                    where { PLAYER_UUID eq FastUUID.toString(player) and (JOB eq job) }
                    rows(SKILL, LOCKED, LEVEL)
                }.map {
                    PlayerSkillPO(player, job, getString(SKILL), getBoolean(LOCKED), getInt(LEVEL))
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

    override fun getPlayerKey(player: UUID): CompletableFuture<PlayerKeySettingPO?> {
        debug("Mysql 获取玩家 KeySetting")
        val future = CompletableFuture<PlayerKeySettingPO?>()
        fun read() {
            try {
                future.complete(keyTable.select(dataSource) {
                    where { PLAYER_UUID eq FastUUID.toString(player) }
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

    override fun savePlayerData(player: UUID, playerProfilePO: PlayerProfilePO, onSuccess: () -> Unit) {
        debug("Mysql 保存玩家 Profile")
        playerTable.transaction(dataSource) {
            insert(PLAYER_UUID, JOB, POINT, FLAGS) {
                onDuplicateKeyUpdate {
                    update(JOB, playerProfilePO.job ?: "null")
                    update(POINT, playerProfilePO.point)
                    update(FLAGS, Json.encodeToString(playerProfilePO.flags))
                }
                value(
                    FastUUID.toString(player),
                    playerProfilePO.job,
                    playerProfilePO.point,
                    Json.encodeToString(playerProfilePO.flags)
                )
            }
        }.onSuccess { onSuccess() }
    }

    override fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO, onSuccess: () -> Unit) {
        debug("Mysql 保存玩家 Job")
        jobsTable.transaction(dataSource) {
            insert(PLAYER_UUID, JOB, EXPERIENCE, GROUP, BIND_KEY_OF_GROUP) {
                onDuplicateKeyUpdate {
                    update(EXPERIENCE, playerJobPO.experience)
                    update(GROUP, playerJobPO.group)
                    update(BIND_KEY_OF_GROUP, Json.encodeToString(playerJobPO.bindKeyOfGroup))
                }
                value(
                    FastUUID.toString(player),
                    playerJobPO.job,
                    playerJobPO.experience,
                    playerJobPO.group,
                    Json.encodeToString(playerJobPO.bindKeyOfGroup)
                )
            }
        }.onSuccess { onSuccess() }
    }

    override fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO, onSuccess: () -> Unit) {
        debug("Mysql 保存玩家 Skill")
        skillsTable.transaction(dataSource) {
            insert(PLAYER_UUID, JOB, SKILL, LOCKED, LEVEL) {
                onDuplicateKeyUpdate {
                    update(LOCKED, playerSkillPO.locked)
                    update(LEVEL, playerSkillPO.level)
                }
                value(
                    FastUUID.toString(player),
                    playerSkillPO.job,
                    playerSkillPO.skill,
                    playerSkillPO.locked,
                    playerSkillPO.level
                )
            }
        }.onSuccess { onSuccess() }
    }

    override fun savePlayerKey(player: UUID, playerKeySettingPO: PlayerKeySettingPO, onSuccess: () -> Unit) {
        debug("Mysql 保存玩家 KeySetting")
        keyTable.transaction(dataSource) {
            insert(PLAYER_UUID, KEY_SETTING) {
                onDuplicateKeyUpdate {
                    update(KEY_SETTING, Json.encodeToString(playerKeySettingPO))
                }
                value(
                    FastUUID.toString(player),
                    Json.encodeToString(playerKeySettingPO)
                )
            }
        }.onSuccess { onSuccess() }
    }
}