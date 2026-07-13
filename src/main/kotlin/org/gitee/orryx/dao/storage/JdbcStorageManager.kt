package org.gitee.orryx.dao.storage

import kotlinx.serialization.json.Json
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.core.profile.SerializableFlag
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.utils.toFlag
import org.gitee.orryx.utils.toSerializable
import org.gitee.orryx.utils.uuidToBytes
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.sql.DataSource

/** 非阻塞关闭屏障：停止接收新操作，等待全部已接收 Future，再执行关闭动作。 */
internal class AsyncCloseBarrier(private val closingMessage: String) {

    private enum class State { OPEN, CLOSING, CLOSED }

    private val lock = Any()
    private val inFlight = LinkedHashSet<CompletableFuture<Unit>>()
    private var state = State.OPEN
    private var firstFailure: Throwable? = null
    private var closeFuture: CompletableFuture<Unit>? = null

    fun <T> submit(operation: () -> CompletionStage<T>): CompletableFuture<T> {
        val result = CompletableFuture<T>()
        val ticket = CompletableFuture<Unit>()
        synchronized(lock) {
            if (state != State.OPEN) {
                result.completeExceptionally(IllegalStateException(closingMessage))
                return result
            }
            inFlight += ticket
        }

        val stage = try {
            operation()
        } catch (throwable: Throwable) {
            CompletableFuture<T>().also { it.completeExceptionally(throwable) }
        }
        stage.whenComplete { value, throwable ->
            synchronized(lock) {
                if (throwable != null && state != State.OPEN && firstFailure == null) {
                    firstFailure = unwrap(throwable)
                }
                inFlight.remove(ticket)
            }
            if (throwable == null) {
                result.complete(value)
            } else {
                result.completeExceptionally(throwable)
            }
            ticket.complete(Unit)
        }
        return result
    }

    fun close(action: () -> CompletionStage<Unit>): CompletableFuture<Unit> {
        val result: CompletableFuture<Unit>
        val accepted: Array<CompletableFuture<Unit>>
        synchronized(lock) {
            closeFuture?.let { return it }
            state = State.CLOSING
            result = CompletableFuture()
            closeFuture = result
            accepted = inFlight.toTypedArray()
        }

        CompletableFuture.allOf(*accepted).whenComplete { _, _ ->
            val closeStage = try {
                action()
            } catch (throwable: Throwable) {
                CompletableFuture<Unit>().also { it.completeExceptionally(throwable) }
            }
            closeStage.whenComplete { _, closeFailure ->
                val failure = synchronized(lock) {
                    state = State.CLOSED
                    combineFailures(firstFailure, closeFailure)
                }
                if (failure == null) result.complete(Unit) else result.completeExceptionally(failure)
            }
        }
        return result
    }

    private fun combineFailures(first: Throwable?, second: Throwable?): Throwable? {
        val primary = first ?: second?.let(::unwrap) ?: return null
        val additional = second?.let(::unwrap)
        if (additional != null && additional !== primary) primary.addSuppressed(additional)
        return primary
    }

    private fun unwrap(throwable: Throwable): Throwable {
        var current = throwable
        while (current is CompletionException && current.cause != null) {
            current = current.cause ?: break
        }
        return current
    }
}

/**
 * 三种内置数据库共用的 JDBC 实现。
 *
 * 所有连接与 SQL 都只会在专用 I/O executor 上运行；SQLite/H2 使用单线程 executor，
 * 避免不同玩家并行写入造成文件数据库锁竞争。
 */
internal class JdbcStorageManager(
    private val dataSource: DataSource,
    private val dialect: Dialect,
) : IStorageManager {

    internal enum class Dialect {
        MYSQL,
        SQLITE,
        H2,
    }

    private val executor: ExecutorService = if (dialect == Dialect.MYSQL) {
        Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "orryx-mysql").apply { isDaemon = false }
        }
    } else {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "orryx-${dialect.name.lowercase()}").apply { isDaemon = false }
        }
    }

    private val lifecycle = AsyncCloseBarrier("Orryx 数据库存储正在关闭")

    private val ready: CompletableFuture<Unit> = CompletableFuture.supplyAsync({
        createTables()
        Unit
    }, executor)

    override fun initializeAsync(): CompletableFuture<Unit> = ready

    override fun closeAsync(): CompletableFuture<Unit> {
        return lifecycle.close {
            ready.handle { _, initializeFailure -> initializeFailure }.thenCompose { initializeFailure ->
                val closeStage = try {
                    CompletableFuture.supplyAsync({
                        (dataSource as? AutoCloseable)?.close()
                        Unit
                    }, executor)
                } catch (throwable: Throwable) {
                    CompletableFuture<Unit>().also { it.completeExceptionally(throwable) }
                }
                closeStage.handle { _, closeFailure ->
                    val failure = initializeFailure ?: closeFailure
                    if (initializeFailure != null && closeFailure != null && closeFailure !== initializeFailure) {
                        initializeFailure.addSuppressed(closeFailure)
                    }
                    if (failure != null) throw CompletionException(failure)
                    Unit
                }
            }
        }.whenComplete { _, _ -> executor.shutdown() }
    }

    private fun <T> read(block: () -> T): CompletableFuture<T> {
        return lifecycle.submit {
            ready.thenCompose {
                CompletableFuture.supplyAsync(block, executor)
            }
        }
    }

    private fun write(block: () -> Unit): CompletableFuture<Unit> = read {
        block()
        Unit
    }

    private fun createTables() {
        dataSource.connection.use { connection ->
            schemaStatements().forEach { sql ->
                connection.createStatement().use { statement -> statement.execute(sql) }
            }
        }
    }

    private fun schemaStatements(): List<String> {
        val identity = when (dialect) {
            Dialect.MYSQL -> "INT AUTO_INCREMENT PRIMARY KEY"
            Dialect.SQLITE -> "INTEGER PRIMARY KEY AUTOINCREMENT"
            Dialect.H2 -> "INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY"
        }
        val uuidType = if (dialect == Dialect.SQLITE) "TEXT" else "VARBINARY(16)"
        val boolType = if (dialect == Dialect.SQLITE) "INTEGER" else "BOOLEAN"
        return listOf(
            """CREATE TABLE IF NOT EXISTS `orryx_player` (
                `id` $identity,
                `uuid` $uuidType NOT NULL UNIQUE,
                `job` VARCHAR(255),
                `point` INTEGER NOT NULL DEFAULT 0,
                `flags` TEXT
            )""".trimIndent(),
            """CREATE TABLE IF NOT EXISTS `orryx_player_jobs` (
                `id` INTEGER NOT NULL,
                `job` VARCHAR(255) NOT NULL,
                `experiences` INTEGER NOT NULL DEFAULT 0,
                `group` VARCHAR(255) NOT NULL,
                `bind_key_of_group` TEXT NOT NULL,
                PRIMARY KEY (`id`, `job`)
            )""".trimIndent(),
            """CREATE TABLE IF NOT EXISTS `orryx_player_job_skills` (
                `id` INTEGER NOT NULL,
                `job` VARCHAR(255) NOT NULL,
                `skill` VARCHAR(255) NOT NULL,
                `locked` $boolType NOT NULL,
                `level` INTEGER NOT NULL,
                PRIMARY KEY (`id`, `job`, `skill`)
            )""".trimIndent(),
            """CREATE TABLE IF NOT EXISTS `orryx_player_key_setting` (
                `id` INTEGER PRIMARY KEY,
                `key_setting` TEXT NOT NULL
            )""".trimIndent(),
            """CREATE TABLE IF NOT EXISTS `orryx_global_flag` (
                `key` VARCHAR(255) PRIMARY KEY,
                `flag` TEXT,
                `deleted` $boolType NOT NULL DEFAULT ${if (dialect == Dialect.SQLITE) 0 else "FALSE"}
            )""".trimIndent(),
        )
    }

    override fun getPlayerData(player: UUID): CompletableFuture<PlayerProfilePO> = read {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                var result = selectProfile(connection, player)
                if (result == null) {
                    try {
                        connection.prepareStatement(
                            "INSERT INTO `orryx_player` (`uuid`, `job`, `point`, `flags`) VALUES (?, ?, ?, ?)"
                        ).use { statement ->
                            setUuid(statement, 1, player)
                            statement.setString(2, null)
                            statement.setInt(3, 0)
                            statement.setString(4, null)
                            statement.executeUpdate()
                        }
                    } catch (_: Throwable) {
                        // 多服或并发首次加载可能已由另一请求创建；随后重新读取唯一记录。
                    }
                    result = selectProfile(connection, player)
                }
                connection.commit()
                result ?: error("创建玩家 Profile 后仍无法读取: $player")
            } catch (throwable: Throwable) {
                connection.rollback()
                throw throwable
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun selectProfile(connection: Connection, player: UUID): PlayerProfilePO? {
        connection.prepareStatement(
            "SELECT `id`, `job`, `point`, `flags` FROM `orryx_player` WHERE `uuid` = ? LIMIT 1"
        ).use { statement ->
            setUuid(statement, 1, player)
            statement.executeQuery().use { result ->
                if (!result.next()) return null
                return PlayerProfilePO(
                    result.getInt("id"),
                    player,
                    result.getString("job"),
                    result.getInt("point"),
                    result.getString("flags")?.takeIf { it.isNotBlank() }
                        ?.let { Json.decodeFromString(it) }
                        ?: emptyMap(),
                )
            }
        }
    }

    override fun getPlayerJob(player: UUID, id: Int, job: String): CompletableFuture<PlayerJobPO?> = read {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT `experiences`, `group`, `bind_key_of_group` FROM `orryx_player_jobs` WHERE `id` = ? AND `job` = ? LIMIT 1"
            ).use { statement ->
                statement.setInt(1, id)
                statement.setString(2, job)
                statement.executeQuery().use { result ->
                    if (!result.next()) return@read null
                    PlayerJobPO(
                        id,
                        player,
                        job,
                        result.getInt("experiences"),
                        result.getString("group"),
                        Json.decodeFromString(result.getString("bind_key_of_group")),
                    )
                }
            }
        }
    }

    override fun getPlayerSkill(player: UUID, id: Int, job: String, skill: String): CompletableFuture<PlayerSkillPO?> = read {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT `locked`, `level` FROM `orryx_player_job_skills` WHERE `id` = ? AND `job` = ? AND `skill` = ? LIMIT 1"
            ).use { statement ->
                statement.setInt(1, id)
                statement.setString(2, job)
                statement.setString(3, skill)
                statement.executeQuery().use { result ->
                    if (!result.next()) return@read null
                    PlayerSkillPO(id, player, job, skill, result.getBoolean("locked"), result.getInt("level"))
                }
            }
        }
    }

    override fun getPlayerSkills(player: UUID, id: Int, job: String): CompletableFuture<List<PlayerSkillPO>> = read {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT `skill`, `locked`, `level` FROM `orryx_player_job_skills` WHERE `id` = ? AND `job` = ?"
            ).use { statement ->
                statement.setInt(1, id)
                statement.setString(2, job)
                statement.executeQuery().use { result ->
                    buildList {
                        while (result.next()) {
                            add(
                                PlayerSkillPO(
                                    id,
                                    player,
                                    job,
                                    result.getString("skill"),
                                    result.getBoolean("locked"),
                                    result.getInt("level"),
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun getPlayerKey(id: Int): CompletableFuture<PlayerKeySettingPO?> = read {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT `key_setting` FROM `orryx_player_key_setting` WHERE `id` = ? LIMIT 1"
            ).use { statement ->
                statement.setInt(1, id)
                statement.executeQuery().use { result ->
                    if (!result.next()) return@read null
                    Json.decodeFromString<PlayerKeySettingPO>(result.getString("key_setting"))
                }
            }
        }
    }

    override fun savePlayerDataAsync(playerProfilePO: PlayerProfilePO): CompletableFuture<Unit> = write {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE `orryx_player` SET `job` = ?, `point` = ?, `flags` = ? WHERE `id` = ?"
            ).use { statement ->
                statement.setString(1, playerProfilePO.job)
                statement.setInt(2, playerProfilePO.point)
                statement.setString(3, Json.encodeToString(playerProfilePO.flags))
                statement.setInt(4, playerProfilePO.id)
                statement.executeUpdate()
            }
        }
    }

    override fun savePlayerJobAsync(playerJobPO: PlayerJobPO): CompletableFuture<Unit> = write {
        dataSource.connection.use { connection ->
            transaction(connection) {
                upsertJob(connection, playerJobPO)
            }
        }
    }

    override fun savePlayerSkillAsync(playerSkillPO: PlayerSkillPO): CompletableFuture<Unit> = write {
        dataSource.connection.use { connection ->
            transaction(connection) {
                upsertSkill(connection, playerSkillPO)
            }
        }
    }

    override fun savePlayerDataAndJobAsync(
        profilePO: PlayerProfilePO,
        jobPO: PlayerJobPO,
    ): CompletableFuture<Unit> = write {
        dataSource.connection.use { connection ->
            transaction(connection) {
                connection.prepareStatement(
                    "UPDATE `orryx_player` SET `job` = ?, `point` = ?, `flags` = ? WHERE `id` = ?"
                ).use { statement ->
                    statement.setString(1, profilePO.job)
                    statement.setInt(2, profilePO.point)
                    statement.setString(3, Json.encodeToString(profilePO.flags))
                    statement.setInt(4, profilePO.id)
                    statement.executeUpdate()
                }
                upsertJob(connection, jobPO)
            }
        }
    }

    override fun saveJobAndSkillsAsync(
        jobPO: PlayerJobPO,
        skillPOs: List<PlayerSkillPO>,
    ): CompletableFuture<Unit> = write {
        dataSource.connection.use { connection ->
            transaction(connection) {
                upsertJob(connection, jobPO)
                skillPOs.forEach { upsertSkill(connection, it) }
            }
        }
    }

    override fun savePlayerKeyAsync(playerKeySettingPO: PlayerKeySettingPO): CompletableFuture<Unit> = write {
        dataSource.connection.use { connection ->
            transaction(connection) {
                val exists = exists(connection, "SELECT 1 FROM `orryx_player_key_setting` WHERE `id` = ?", playerKeySettingPO.id)
                val json = Json.encodeToString(playerKeySettingPO)
                if (exists) {
                    connection.prepareStatement(
                        "UPDATE `orryx_player_key_setting` SET `key_setting` = ? WHERE `id` = ?"
                    ).use { statement ->
                        statement.setString(1, json)
                        statement.setInt(2, playerKeySettingPO.id)
                        statement.executeUpdate()
                    }
                } else {
                    connection.prepareStatement(
                        "INSERT INTO `orryx_player_key_setting` (`id`, `key_setting`) VALUES (?, ?)"
                    ).use { statement ->
                        statement.setInt(1, playerKeySettingPO.id)
                        statement.setString(2, json)
                        statement.executeUpdate()
                    }
                }
            }
        }
    }

    override fun getGlobalFlag(key: String): CompletableFuture<IFlag?> = read {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT `flag` FROM `orryx_global_flag` WHERE `key` = ? AND `deleted` = ? LIMIT 1"
            ).use { statement ->
                statement.setString(1, key)
                statement.setBoolean(2, false)
                statement.executeQuery().use { result ->
                    if (!result.next()) return@read null
                    Json.decodeFromString<SerializableFlag>(result.getString("flag")).toFlag()
                }
            }
        }
    }

    override fun saveGlobalFlagAsync(key: String, flag: IFlag?): CompletableFuture<Unit> = write {
        dataSource.connection.use { connection ->
            transaction(connection) {
                val exists = exists(connection, "SELECT 1 FROM `orryx_global_flag` WHERE `key` = ?", key)
                if (flag == null) {
                    if (exists) {
                        connection.prepareStatement(
                            "UPDATE `orryx_global_flag` SET `deleted` = ? WHERE `key` = ?"
                        ).use { statement ->
                            statement.setBoolean(1, true)
                            statement.setString(2, key)
                            statement.executeUpdate()
                        }
                    }
                } else {
                    val json = Json.encodeToString(flag.toSerializable())
                    if (exists) {
                        connection.prepareStatement(
                            "UPDATE `orryx_global_flag` SET `flag` = ?, `deleted` = ? WHERE `key` = ?"
                        ).use { statement ->
                            statement.setString(1, json)
                            statement.setBoolean(2, false)
                            statement.setString(3, key)
                            statement.executeUpdate()
                        }
                    } else {
                        connection.prepareStatement(
                            "INSERT INTO `orryx_global_flag` (`key`, `flag`, `deleted`) VALUES (?, ?, ?)"
                        ).use { statement ->
                            statement.setString(1, key)
                            statement.setString(2, json)
                            statement.setBoolean(3, false)
                            statement.executeUpdate()
                        }
                    }
                }
            }
        }
    }

    private fun upsertJob(connection: Connection, value: PlayerJobPO) {
        val exists = exists(
            connection,
            "SELECT 1 FROM `orryx_player_jobs` WHERE `id` = ? AND `job` = ?",
            value.id,
            value.job,
        )
        if (exists) {
            connection.prepareStatement(
                "UPDATE `orryx_player_jobs` SET `experiences` = ?, `group` = ?, `bind_key_of_group` = ? WHERE `id` = ? AND `job` = ?"
            ).use { statement ->
                statement.setInt(1, value.experience)
                statement.setString(2, value.group)
                statement.setString(3, Json.encodeToString(value.bindKeyOfGroup))
                statement.setInt(4, value.id)
                statement.setString(5, value.job)
                statement.executeUpdate()
            }
        } else {
            connection.prepareStatement(
                "INSERT INTO `orryx_player_jobs` (`id`, `job`, `experiences`, `group`, `bind_key_of_group`) VALUES (?, ?, ?, ?, ?)"
            ).use { statement ->
                statement.setInt(1, value.id)
                statement.setString(2, value.job)
                statement.setInt(3, value.experience)
                statement.setString(4, value.group)
                statement.setString(5, Json.encodeToString(value.bindKeyOfGroup))
                statement.executeUpdate()
            }
        }
    }

    private fun upsertSkill(connection: Connection, value: PlayerSkillPO) {
        val exists = exists(
            connection,
            "SELECT 1 FROM `orryx_player_job_skills` WHERE `id` = ? AND `job` = ? AND `skill` = ?",
            value.id,
            value.job,
            value.skill,
        )
        if (exists) {
            connection.prepareStatement(
                "UPDATE `orryx_player_job_skills` SET `locked` = ?, `level` = ? WHERE `id` = ? AND `job` = ? AND `skill` = ?"
            ).use { statement ->
                statement.setBoolean(1, value.locked)
                statement.setInt(2, value.level)
                statement.setInt(3, value.id)
                statement.setString(4, value.job)
                statement.setString(5, value.skill)
                statement.executeUpdate()
            }
        } else {
            connection.prepareStatement(
                "INSERT INTO `orryx_player_job_skills` (`id`, `job`, `skill`, `locked`, `level`) VALUES (?, ?, ?, ?, ?)"
            ).use { statement ->
                statement.setInt(1, value.id)
                statement.setString(2, value.job)
                statement.setString(3, value.skill)
                statement.setBoolean(4, value.locked)
                statement.setInt(5, value.level)
                statement.executeUpdate()
            }
        }
    }

    private fun exists(connection: Connection, sql: String, vararg values: Any): Boolean {
        return connection.prepareStatement(sql).use { statement ->
            values.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
            statement.executeQuery().use { result -> result.next() }
        }
    }

    private fun setUuid(statement: PreparedStatement, index: Int, player: UUID) {
        if (dialect == Dialect.SQLITE) {
            statement.setString(index, player.toString())
        } else {
            statement.setBytes(index, uuidToBytes(player))
        }
    }

    private inline fun transaction(connection: Connection, block: () -> Unit) {
        val originalAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            block()
            connection.commit()
        } catch (throwable: Throwable) {
            connection.rollback()
            throw throwable
        } finally {
            connection.autoCommit = originalAutoCommit
        }
    }
}
