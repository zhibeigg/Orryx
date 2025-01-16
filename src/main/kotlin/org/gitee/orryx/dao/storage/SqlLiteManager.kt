package org.gitee.orryx.dao.storage

import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.dao.pojo.PlayerData
import org.gitee.orryx.dao.pojo.PlayerJob
import org.gitee.orryx.dao.pojo.PlayerSkill
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
        primaryKeyForLegacy += listOf(UUID, JOB)
    }

    private val skillsTable: Table<*, *> = Table("orryx_player_job_skills", host) {
        add(UUID) { type(ColumnTypeSQLite.TEXT) }
        add(JOB) { type(ColumnTypeSQLite.TEXT) }
        add(SKILL) { type(ColumnTypeSQLite.TEXT) }
        add(LOCKED) { type(ColumnTypeSQLite.BLOB) }
        add(LEVEL) { type(ColumnTypeSQLite.INTEGER) }
        add(BIND_KEY_OF_GROUP) { type(ColumnTypeSQLite.TEXT) }
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
                gson.fromJson<Map<String, IFlag<*>>>(getString(FLAGS), Map::class.java)
            )
        }
    }

    override fun getPlayerJob(player: UUID, job: String): PlayerJob? {
        return jobsTable.select(dataSource) {
            where { UUID eq player.toString() and ( JOB eq job ) }
            rows(EXPERIENCE, GROUP)
        }.firstOrNull {
            PlayerJob(player, job, getInt(EXPERIENCE), getString(GROUP))
        }
    }

    override fun getPlayerSkill(player: UUID, job: String, skill: String): PlayerSkill? {
        return skillsTable.select(dataSource) {
            where { UUID eq player.toString() and ( JOB eq job ) and ( SKILL eq skill ) }
            rows(LOCKED, LEVEL, BIND_KEY_OF_GROUP)
        }.firstOrNull {
            PlayerSkill(player, job, skill, getBoolean(LOCKED), getInt(LEVEL), gson.fromJson<Map<String, String?>>(getString(
                FLAGS
            ), Map::class.java))
        }
    }

    override fun savePlayerData(player: UUID, playerData: PlayerData) {
        playerTable.insert(dataSource, UUID, JOB, POINT, FLAGS) {
            onDuplicateKeyUpdate {
                update(POINT, playerData.point)
                update(FLAGS, gson.toJson(playerData.flags))
                value()
            }
            value(player.toString(), playerData.job, playerData.point, gson.toJson(playerData.flags))
        }
    }

    override fun savePlayerJob(player: UUID, playerJob: PlayerJob) {
        jobsTable.insert(dataSource, UUID, JOB, EXPERIENCE, GROUP) {
            onDuplicateKeyUpdate {
                update(EXPERIENCE, playerJob.experience)
                update(GROUP, playerJob.group)
            }
            value(player.toString(), playerJob.job, playerJob.experience, playerJob.group)
        }
    }

    override fun savePlayerSkill(player: UUID, playerSkill: PlayerSkill) {
        skillsTable.insert(dataSource, UUID, JOB, SKILL, LOCKED, LEVEL, BIND_KEY_OF_GROUP) {
            onDuplicateKeyUpdate {
                update(LOCKED, playerSkill.locked)
                update(LEVEL, playerSkill.level)
                update(BIND_KEY_OF_GROUP, gson.toJson(playerSkill.bindKeyOfGroup))
            }
            value(player.toString(), playerSkill.job, playerSkill.skill, playerSkill.locked, playerSkill.level, gson.toJson(playerSkill.bindKeyOfGroup))
        }
    }

}