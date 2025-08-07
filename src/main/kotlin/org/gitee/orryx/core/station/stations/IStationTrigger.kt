package org.gitee.orryx.core.station.stations

import org.gitee.orryx.core.station.ITrigger
import taboolib.common.platform.ProxyCommandSender

interface IStationTrigger<E>: ITrigger<E> {

    /**
     * 中转站需要读取的特殊配置文件键
     * */
    val specialKeys: Array<String>

    /**
     * 注册时预先触发
     * */
    fun onRegister(station: IStation, map: Map<String, Any?>)

    /**
     * 当检测通过时
     * @param event 监听到的事件
     * @param map 传入的特殊参数
     * @return 执行Script的Sender
     * */
    fun onJoin(event: E, map: Map<String, Any?>): ProxyCommandSender

    /**
     * 检测事件是否具备进入中转站条件时
     * @param station 中转站
     * @param event 监听到的事件
     * @param map 传入的特殊参数
     * @return 是否能进入
     * */
    fun onCheck(station: IStation, event: E, map: Map<String, Any?>): Boolean
}