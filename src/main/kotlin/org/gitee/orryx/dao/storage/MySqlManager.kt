package org.gitee.orryx.dao.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submitAsync
import taboolib.common.util.unsafeLazy
import taboolib.module.database.ColumnOptionSQL
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.Table
import taboolib.module.database.getHost
import java.util.*
import java.util.concurrent.CompletableFuture

class MySqlManager: IStorageManager {

    private val host by unsafeLazy { Orryx.config.getHost("Database.sql") }
    private val dataSource by unsafeLazy { host.createDataSource() }

    private val playerTable: Table<*, *> = Table("orryx_player", host) {
        add(UUID) { type(ColumnTypeSQL.CHAR, 36) { options(ColumnOptionSQL.PRIMARY_KEY) } }
        add(JOB) { type(ColumnTypeSQL.VARCHAR, 255) }
        add(POINT) { type(ColumnTypeSQL.INT) }
        add(FLAGS) { type(ColumnTypeSQL.TEXT) }
    }

    private val jobsTable: Table<*, *> = Table("orryx_player_jobs", host) {
        add(UUID) { type(ColumnTypeSQL.CHAR, 36) }
        add(JOB) { type(ColumnTypeSQL.VARCHAR, 255) }
        add(EXPERIENCE) { type(ColumnTypeSQL.INT) }
        add(GROUP) { type(ColumnTypeSQL.VARCHAR, 255) }
        add(BIND_KEY_OF_GROUP) { type(ColumnTypeSQL.TEXT) }
        primaryKeyForLegacy += listOf(UUID, JOB)
    }

    private val skillsTable: Table<*, *> = Table("orryx_player_job_skills", host) {
        add(UUID) { type(ColumnTypeSQL.CHAR, 36) }
        add(JOB) { type(ColumnTypeSQL.VARCHAR, 255) }
        add(SKILL) { type(ColumnTypeSQL.VARCHAR, 255) }
        add(LOCKED) { type(ColumnTypeSQL.BOOLEAN) }
        add(LEVEL) { type(ColumnTypeSQL.INT) }
        primaryKeyForLegacy += listOf(UUID, JOB, SKILL)
    }

    private val keyTable: Table<*, *> = Table("orryx_player_key_setting", host) {
        add(UUID) { type(ColumnTypeSQL.CHAR, 36) { options(ColumnOptionSQL.PRIMARY_KEY) } }
        add(KEY_SETTING) { type(ColumnTypeSQL.TEXT) }
    }

    init {
        playerTable.createTable(dataSource)
        jobsTable.createTable(dataSource)
        skillsTable.createTable(dataSource)
        keyTable.createTable(dataSource)
    }

    override fun getPlayerData(player: UUID): CompletableFuture<PlayerProfilePO?> {
        val future = CompletableFuture<PlayerProfilePO?>()
        fun read() {
            try {
                future.complete(playerTable.select(dataSource) {
                    where { UUID eq player.toString() }
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
        val future = CompletableFuture<PlayerJobPO?>()
        fun read() {
            try {
                future.complete(jobsTable.select(dataSource) {
                    where { UUID eq player.toString() and (JOB eq job) }
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
        val future = CompletableFuture<PlayerSkillPO?>()
        fun read() {
            try {
                future.complete(skillsTable.select(dataSource) {
                    where { UUID eq player.toString() and (JOB eq job) and (SKILL eq skill) }
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
        val future = CompletableFuture<List<PlayerSkillPO>>()
        fun read() {
            try {
                future.complete(skillsTable.select(dataSource) {
                    where { UUID eq player.toString() and (JOB eq job) }
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

    override fun getPlayerKey(player: UUID): CompletableFuture<PlayerKeySettingPO> {
        val future = CompletableFuture<PlayerKeySettingPO>()
        fun read() {
            try {
                future.complete(keyTable.select(dataSource) {
                    where { UUID eq player.toString() }
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
        playerTable.transaction(dataSource) {
            insert(UUID, JOB, POINT, FLAGS) {
                onDuplicateKeyUpdate {
                    update(POINT, playerProfilePO.point)
                    update(FLAGS, Json.encodeToString(playerProfilePO.flags))
                }
                value(
                    player.toString(),
                    playerProfilePO.job,
                    playerProfilePO.point,
                    Json.encodeToString(playerProfilePO.flags)
                )
            }
        }.onSuccess { onSuccess() }
    }

    override fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO, onSuccess: () -> Unit) {
        jobsTable.transaction(dataSource) {
            insert(UUID, JOB, EXPERIENCE, GROUP, BIND_KEY_OF_GROUP) {
                onDuplicateKeyUpdate {
                    update(EXPERIENCE, playerJobPO.experience)
                    update(GROUP, playerJobPO.group)
                    update(BIND_KEY_OF_GROUP, Json.encodeToString(playerJobPO.bindKeyOfGroup))
                }
                value(
                    player.toString(),
                    playerJobPO.job,
                    playerJobPO.experience,
                    playerJobPO.group,
                    Json.encodeToString(playerJobPO.bindKeyOfGroup)
                )
            }
        }.onSuccess { onSuccess() }
    }

    override fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO, onSuccess: () -> Unit) {
        skillsTable.transaction(dataSource) {
            insert(UUID, JOB, SKILL, LOCKED, LEVEL) {
                onDuplicateKeyUpdate {
                    update(LOCKED, playerSkillPO.locked)
                    update(LEVEL, playerSkillPO.level)
                }
                value(
                    player.toString(),
                    playerSkillPO.job,
                    playerSkillPO.skill,
                    playerSkillPO.locked,
                    playerSkillPO.level
                )
            }
        }.onSuccess { onSuccess() }
    }

    override fun savePlayerKey(player: UUID, playerKeySettingPO: PlayerKeySettingPO, onSuccess: () -> Unit) {
        keyTable.transaction(dataSource) {
            insert(UUID, KEY_SETTING) {
                onDuplicateKeyUpdate {
                    update(KEY_SETTING, Json.encodeToString(playerKeySettingPO))
                }
                value(
                    player.toString(),
                    Json.encodeToString(playerKeySettingPO)
                )
            }
        }.onSuccess { onSuccess() }
    }

}