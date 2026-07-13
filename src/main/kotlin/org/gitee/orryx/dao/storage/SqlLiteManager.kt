package org.gitee.orryx.dao.storage

import taboolib.common.io.newFile
import taboolib.module.database.getHost
import java.util.UUID

/** SQLite 存储实现；通过单线程 JDBC executor 串行所有文件数据库操作。 */
class SqlLiteManager : IStorageManager by JdbcStorageManager(
    newFile(IStorageManager.file, "data.db").getHost().createDataSource(),
    JdbcStorageManager.Dialect.SQLITE,
) {

    /** 玩家写入通道已经在空闲后释放，SQLite 不再维护可被误删的玩家锁。 */
    fun quit(@Suppress("UNUSED_PARAMETER") player: UUID) = Unit
}
