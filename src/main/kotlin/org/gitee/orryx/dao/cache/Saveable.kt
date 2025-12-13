package org.gitee.orryx.dao.cache

import org.gitee.orryx.utils.EMPTY_RUNNABLE
import taboolib.common.platform.function.isPrimaryThread

interface Saveable {

    /**
     * 保存数据
     * @param async 是否异步
     * @param remove 是否删除缓存
     * @param callback 完成回调
     * */
    fun save(async: Boolean = isPrimaryThread, remove: Boolean = true, callback: Runnable = EMPTY_RUNNABLE)
}