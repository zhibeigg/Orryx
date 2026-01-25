package org.gitee.orryx.api

import org.gitee.orryx.api.interfaces.IOrryxAPI
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile

object Orryx {

    @Config("config.yml")
    lateinit var config: ConfigFile
        private set

    private var api: IOrryxAPI? = null

    /**
     * 注册开发者接口
     */
    fun register(api: IOrryxAPI) {
        this.api = api
    }

    /**
     * 获取开发者接口
     */
    fun api(): IOrryxAPI {
        return api ?: error("OrryxAPI has not finished loading, or failed to load!")
    }
}