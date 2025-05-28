package org.gitee.orryx.dao.storage

import taboolib.common.io.newFile
import taboolib.module.database.HostH2

class H2Manager: IStorageManager by MySqlManager(HostH2(newFile(IStorageManager.file, "data", false)).createDataSource())