package org.gitee.orryx.dao.cache

import org.gitee.orryx.utils.EMPTY_RUNNABLE
import taboolib.common.platform.function.isPrimaryThread

interface Saveable {

    /**
     * 保存数据（将当前对象的状态持久化到存储介质）。
     *
     * 约定：
     * - 当 [async] 为 true 时，实现应在异步线程执行 I/O，避免阻塞主线程。
     * - 当 [async] 为 false 时，允许在当前线程执行保存逻辑，但需保证不会引起长时间卡顿。
     * - [callback] 用于通知保存流程已结束（成功或失败均视为结束）。
     *
     * 实现建议：
     * - 若保存失败，请在日志中记录异常，并确保 [callback] 仍被调用。
     * - 如需线程切换，请明确回调触发的线程语义（主线程或异步线程）。
     *
     * @param async 是否异步执行保存，默认在主线程时为 true
     * @param remove 保存完成后是否从缓存移除；若为 false，缓存可能仍为旧状态，读取不一定立刻反映最新数据
     * @param callback 保存完成后的回调
     */
    fun save(async: Boolean = isPrimaryThread, remove: Boolean = true, callback: Runnable = EMPTY_RUNNABLE)
}
