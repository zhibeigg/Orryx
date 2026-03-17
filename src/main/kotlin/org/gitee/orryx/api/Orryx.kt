package org.gitee.orryx.api

import org.gitee.orryx.api.interfaces.IOrryxAPI
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile

object Orryx {

    @Config("config.yml")
    lateinit var config: ConfigFile
        private set

    private var api: IOrryxAPI? = null
    private var registered = false

    /**
     * 注册开发者接口
     *
     * @throws IllegalStateException 如果已经注册过
     */
    fun register(api: IOrryxAPI) {
        check(!registered) { "OrryxAPI 已经注册，不允许重复注册！" }
        this.api = api
        this.registered = true
    }

    /**
     * 获取开发者接口
     */
    fun api(): IOrryxAPI {
        return api ?: error("OrryxAPI has not finished loading, or failed to load!")
    }
}