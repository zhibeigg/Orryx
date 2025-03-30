package org.gitee.orryx.api

import org.gitee.orryx.api.interfaces.IOrryxAPI
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile

@RuntimeDependencies(
    RuntimeDependency(
        "!com.github.ben-manes.caffeine:caffeine:2.9.3",
        test = "!org.gitee.orryx.caffeine.cache.Caffeine",
        relocate = ["!com.github.benmanes.caffeine", "!org.gitee.orryx.caffeine"],
        transitive = false
    ),
    RuntimeDependency(
        "!org.joml:joml:1.10.7",
        test = "!org.gitee.orryx.joml.Math",
        relocate = ["!org.joml", "!org.gitee.orryx.joml"],
        transitive = false
    ),
    RuntimeDependency(
        "!com.larksuite.oapi:oapi-sdk:2.4.7",
        test = "!org.gitee.orryx.larksuite.oapi.Client",
        relocate = ["!com.larksuite.oapi", "!org.gitee.orryx.larksuite.oapi"],
        transitive = false
    )
)
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