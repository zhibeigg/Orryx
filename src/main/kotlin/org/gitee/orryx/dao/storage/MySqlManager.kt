package org.gitee.orryx.dao.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.dao.pojo.PlayerData
import org.gitee.orryx.dao.pojo.PlayerJob
import org.gitee.orryx.dao.pojo.PlayerSkill
import org.gitee.orryx.utils.*
import taboolib.module.database.ColumnOptionSQL
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.Table
import taboolib.module.database.getHost
import java.util.*

class MySqlManager: IStorageManager {

    private val host by lazy { OrryxAPI.config.getHost("Database.sql") }
    private val dataSource by lazy { host.createDataSource() }

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

    init {
        playerTable.createTable(dataSource)
        jobsTable.createTable(dataSource)
        skillsTable.createTable(dataSource)
    }

    override fun getPlayerData(player: UUID): PlayerData? {
        return playerTable.select(dataSource) {
            where { UUID eq player.toString() }
            rows(JOB, POINT, FLAGS)
        }.firstOrNull {
            PlayerData(
                player,
                getString(JOB),
                getInt(POINT),
                Json.decodeFromString(getString(FLAGS))
            )
        }
    }

    override fun getPlayerJob(player: UUID, job: String): PlayerJob? {
        return jobsTable.select(dataSource) {
            where { UUID eq player.toString() and ( JOB eq job ) }
            rows(EXPERIENCE, GROUP, BIND_KEY_OF_GROUP)
        }.firstOrNull {
            PlayerJob(player, job, getInt(EXPERIENCE), getString(GROUP), Json.decodeFromString(getString(BIND_KEY_OF_GROUP)))
        }
    }

    override fun getPlayerSkill(player: UUID, job: String, skill: String): PlayerSkill? {
        return skillsTable.select(dataSource) {
            where { UUID eq player.toString() and ( JOB eq job ) and ( SKILL eq skill ) }
            rows(LOCKED, LEVEL)
        }.firstOrNull {
            PlayerSkill(player, job, skill, getBoolean(LOCKED), getInt(LEVEL))
        }
    }

    override fun savePlayerData(player: UUID, playerData: PlayerData) {
        playerTable.insert(dataSource, UUID, JOB, POINT, FLAGS) {
            onDuplicateKeyUpdate {
                update(POINT, playerData.point)
                update(FLAGS, Json.encodeToString(playerData.flags))
            }
            value(player.toString(), playerData.job, playerData.point, Json.encodeToString(playerData.flags))
        }
    }

    override fun savePlayerJob(player: UUID, playerJob: PlayerJob) {
        jobsTable.insert(dataSource, UUID, JOB, EXPERIENCE, GROUP, BIND_KEY_OF_GROUP) {
            onDuplicateKeyUpdate {
                update(EXPERIENCE, playerJob.experience)
                update(GROUP, playerJob.group)
                update(BIND_KEY_OF_GROUP, Json.encodeToString(playerJob.bindKeyOfGroup))
            }
            value(player.toString(), playerJob.job, playerJob.experience, playerJob.group, Json.encodeToString(playerJob.bindKeyOfGroup))
        }
    }

    override fun savePlayerSkill(player: UUID, playerSkill: PlayerSkill) {
        skillsTable.insert(dataSource, UUID, JOB, SKILL, LOCKED, LEVEL) {
            onDuplicateKeyUpdate {
                update(LOCKED, playerSkill.locked)
                update(LEVEL, playerSkill.level)
            }
            value(player.toString(), playerSkill.job, playerSkill.skill, playerSkill.locked, playerSkill.level)
        }
    }

}