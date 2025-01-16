package org.gitee.orryx.core.station.stations

import taboolib.common.platform.event.EventPriority
import taboolib.module.kether.Script

interface IStation {

    /**
     * 中转站键名
     * */
    val key: String

    /**
     * 中转站监听的事件
     * */
    val event: String

    /**
     * 中转站运行间隔
     * */
    val baffleAction: String?

    /**
     * 中转站监听事件是否跳过被取消事件
     * 默认false
     * */
    val ignoreCancelled: Boolean

    /**
     * 中转站监听事件优先级
     * LOWEST,LOW,NORMAL,HIGH,HIGHEST,MONITOR
     * 默认NORMAL
     * */
    val priority: EventPriority

    /**
     * 中转站处理相同事件的权重
     * 从数字大的开始运行
     * 默认0
     * */
    val weight: Int

    /**
     * 中转站执行语句
     * */
    val actions: String

    /**
     * 中转站的延迟生成变量
     * */
    val variables: Map<String, String>

    /**
     * 技能运行脚本
     * */
    val script: Script?

}