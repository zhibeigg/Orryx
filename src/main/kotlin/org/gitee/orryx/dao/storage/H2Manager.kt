package org.gitee.orryx.dao.storage

import taboolib.common.io.newFile
import taboolib.module.database.HostH2

/** H2 使用自己的 Host、建表方言和单线程 JDBC executor，不再套用 MySQL 配置。 */
class H2Manager : IStorageManager by JdbcStorageManager(
    HostH2(newFile(IStorageManager.file, "data", false)).createDataSource(),
    JdbcStorageManager.Dialect.H2,
)
