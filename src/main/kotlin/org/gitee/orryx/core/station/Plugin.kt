package org.gitee.orryx.core.station

/**
 * 需要依赖其他插件启动
 * @param plugin 需要依赖的插件
 * */
@Target(AnnotationTarget.CLASS)
annotation class Plugin(val plugin: String)
