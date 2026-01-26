package org.gitee.orryx.api.interfaces

/**
 * 重载 API 接口
 *
 * 提供插件配置重载功能
 * */
interface IReloadAPI {

    /**
     * 重载插件配置
     *
     * 重新加载所有配置文件，包括职业、技能、经验计算器等
     * */
    fun reload()
}