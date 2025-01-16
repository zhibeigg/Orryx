package org.gitee.orryx.core.station

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import taboolib.common.platform.ProxyCommandSender
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.extend
import kotlin.reflect.KClass

interface ITrigger<E : Event> {

    /**
     * 监听的事件代名
     * */
    val event: String

    /**
     * 监听的事件
     * */
    val clazz: KClass<E>

    /**
     * 当检测通过时
     * @param event 监听到的事件
     * @param map 传入的特殊参数
     * @return 执行Script的Sender
     * */
    fun onJoin(event: E, map: Map<String, Any?>): ProxyCommandSender

    /**
     * 当开始运行脚本时注入
     * @param context 执行的脚本上下文
     * @param event 监听到的事件
     * @param map 传入的特殊参数
     * */
    fun onStart(context: ScriptContext, event: E, map: Map<String, Any?>) {
        context["@event"] = event
        context.extend(map)
        if (event is Cancellable) {
            context["isCancelled"] = event.isCancelled
        }
    }

    /**
     * 当结束运行脚本时
     * @param context 执行的脚本上下文
     * @param event 监听到的事件
     * @param map 传入的特殊参数
     * */
    fun onEnd(context: ScriptContext, event: E, map: Map<String, Any?>)

}