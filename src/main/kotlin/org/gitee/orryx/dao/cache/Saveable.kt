package org.gitee.orryx.dao.cache

import org.gitee.orryx.utils.EMPTY_FUNCTION
import taboolib.common.platform.function.isPrimaryThread

interface Saveable {

    /**
     * 保存数据
     * @param async 是否异步
     * @param callback 完成回调
     * */
    fun save(async: Boolean = isPrimaryThread, callback: () -> Unit = EMPTY_FUNCTION)

}