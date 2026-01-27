package org.gitee.orryx.core.station.pipe

import org.gitee.orryx.core.station.ITrigger
import taboolib.common.platform.event.ProxyListener

/**
 * 管道任务触发器接口。
 *
 * @param E 监听事件类型
 * @property listener 监听器，在有 PipeTask 使用时注册，无调用时自动注销
 */
interface IPipeTrigger<E>: ITrigger<E> {

    var listener: ProxyListener?

    /**
     * 管式任务中断触发检测。
     *
     * @param pipeTask 管式任务
     * @param event 事件
     * @param map 额外参数
     * @return 是否中断
     */
    fun onCheck(pipeTask: IPipeTask, event: E, map: Map<String, Any?>): Boolean
}
