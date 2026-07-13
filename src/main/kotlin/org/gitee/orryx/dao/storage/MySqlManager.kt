package org.gitee.orryx.dao.storage

import org.gitee.orryx.api.Orryx
import taboolib.module.database.getHost
import javax.sql.DataSource

/** MySQL 存储实现，所有 JDBC 操作由 [JdbcStorageManager] 异步执行。 */
class MySqlManager(replaceDataSource: DataSource? = null) : IStorageManager by JdbcStorageManager(
    replaceDataSource ?: Orryx.config.getHost("Database.sql").createDataSource(),
    JdbcStorageManager.Dialect.MYSQL,
)
