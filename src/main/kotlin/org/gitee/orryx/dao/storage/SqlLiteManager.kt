package org.gitee.orryx.dao.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.utils.*
import taboolib.common.io.newFile
import taboolib.common.platform.function.getDataFolder
import taboolib.module.database.ColumnOptionSQLite
import taboolib.module.database.ColumnTypeSQLite
import taboolib.module.database.Table
import taboolib.module.database.getHost
import java.util.*

class SqlLiteManager: IStorageManager {

    private val host by lazy { newFile(getDataFolder(), "data.db").getHost() }
    private val dataSource by lazy { host.createDataSource() }

    private val playerTable: Table<*, *> = Table("orryx_player", host) {
        add(UUID) { type(ColumnTypeSQLite.TEXT) { options(ColumnOptionSQLite.PRIMARY_KEY) } }
        add(JOB) { type(ColumnTypeSQLite.TEXT) }
        add(POINT) { type(ColumnTypeSQLite.INTEGER) }
        add(FLAGS) { type(ColumnTypeSQLite.TEXT) }
    }

    private val jobsTable: Table<*, *> = Table("orryx_player_jobs", host) {
        add(UUID) { type(ColumnTypeSQLite.TEXT) }
        add(JOB) { type(ColumnTypeSQLite.TEXT) }
        add(EXPERIENCE) { type(ColumnTypeSQLite.INTEGER) }
        add(GROUP) { type(ColumnTypeSQLite.TEXT) }
        add(BIND_KEY_OF_GROUP) { type(ColumnTypeSQLite.TEXT) }
        primaryKeyForLegacy += listOf(UUID, JOB)
    }

    private val skillsTable: Table<*, *> = Table("orryx_player_job_skills", host) {
        add(UUID) { type(ColumnTypeSQLite.TEXT) }
        add(JOB) { type(ColumnTypeSQLite.TEXT) }
        add(SKILL) { type(ColumnTypeSQLite.TEXT) }
        add(LOCKED) { type(ColumnTypeSQLite.BLOB) }
        add(LEVEL) { type(ColumnTypeSQLite.INTEGER) }
        primaryKeyForLegacy += listOf(UUID, JOB, SKILL)
    }

    init {
        playerTable.createTable(dataSource)
        jobsTable.createTable(dataSource)
        skillsTable.createTable(dataSource)
    }

    override fun getPlayerData(player: UUID): PlayerProfilePO? {
        return playerTable.select(dataSource) {
            where { UUID eq player.toString() }
            rows(JOB, POINT, FLAGS)
        }.firstOrNull {
            PlayerProfilePO(
                player,
                getString(JOB),
                getInt(POINT),
                Json.decodeFromString(getString(FLAGS))
            )
        }
    }

    override fun getPlayerJob(player: UUID, job: String): PlayerJobPO? {
        return jobsTable.select(dataSource) {
            where { UUID eq player.toString() and ( JOB eq job ) }
            rows(EXPERIENCE, GROUP, BIND_KEY_OF_GROUP)
        }.firstOrNull {
            PlayerJobPO(player, job, getInt(EXPERIENCE), getString(GROUP), Json.decodeFromString(getString(BIND_KEY_OF_GROUP)))
        }
    }

    override fun getPlayerSkill(player: UUID, job: String, skill: String): PlayerSkillPO? {
        return skillsTable.select(dataSource) {
            where { UUID eq player.toString() and ( JOB eq job ) and ( SKILL eq skill ) }
            rows(LOCKED, LEVEL)
        }.firstOrNull {
            PlayerSkillPO(player, job, skill, getBoolean(LOCKED), getInt(LEVEL))
        }
    }

    override fun savePlayerData(player: UUID, playerProfilePO: PlayerProfilePO) {
        if (playerTable.find(dataSource) { where { UUID eq player.toString() } }) {
            playerTable.update(dataSource) {
                where { UUID eq player.toString() }
                set(POINT, playerProfilePO.point)
                set(FLAGS, Json.encodeToString(playerProfilePO.flags))
            }
        } else {
            playerTable.insert(dataSource, UUID, JOB, POINT, FLAGS) {
                value(player.toString(), playerProfilePO.job, playerProfilePO.point, Json.encodeToString(playerProfilePO.flags))
            }
        }
    }

    override fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO) {
        if (jobsTable.find(dataSource) { where { UUID eq player.toString() and ( JOB eq playerJobPO.job ) } }) {
            jobsTable.update(dataSource) {
                where { UUID eq player.toString() and ( JOB eq playerJobPO.job ) }
                set(EXPERIENCE, playerJobPO.experience)
                set(GROUP, playerJobPO.group)
                set(BIND_KEY_OF_GROUP, Json.encodeToString(playerJobPO.bindKeyOfGroup))
            }
        } else {
            jobsTable.insert(dataSource, UUID, JOB, EXPERIENCE, GROUP, BIND_KEY_OF_GROUP) {
                value(player.toString(), playerJobPO.job, playerJobPO.experience, playerJobPO.group, Json.encodeToString(playerJobPO.bindKeyOfGroup))
            }
        }
    }

    override fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO) {
        if (skillsTable.find(dataSource) { where { UUID eq player.toString() and ( JOB eq playerSkillPO.job ) and ( SKILL eq playerSkillPO.skill ) } }) {
            skillsTable.update(dataSource) {
                where { UUID eq player.toString() and ( JOB eq playerSkillPO.job ) and ( SKILL eq playerSkillPO.skill ) }
                set(LOCKED, playerSkillPO.locked)
                set(LEVEL, playerSkillPO.level)
            }
        } else {
            skillsTable.insert(dataSource, UUID, JOB, SKILL, LOCKED, LEVEL) {
                value(player.toString(), playerSkillPO.job, playerSkillPO.skill, playerSkillPO.locked, playerSkillPO.level)
            }
        }
    }

}