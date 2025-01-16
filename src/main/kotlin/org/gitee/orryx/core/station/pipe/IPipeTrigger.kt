package org.gitee.orryx.core.station.pipe

import org.bukkit.event.Event
import org.gitee.orryx.core.station.ITrigger
import taboolib.common.platform.event.ProxyListener

interface IPipeTrigger<E : Event>: ITrigger<E> {

    /**
     * # 监听器
     * 在有PipeTask使用时注册，在无任何调用时自动注销
     * */
    var listener: ProxyListener?

    /**
     * 管式任务中断触发检测
     * @param pipeTask 管式任务
     * @param event 事件
     * @param map 额外参数
     * @return 是否中断
     * */
    fun onCheck(pipeTask: IPipeTask, event: E, map: Map<String, Any?>): Boolean

}