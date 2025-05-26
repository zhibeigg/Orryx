package org.gitee.orryx.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.gitee.orryx.api.interfaces.*
import org.gitee.orryx.utils.async
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.PlatformFactory
import taboolib.module.kether.KetherScriptLoader

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
    ),
    RuntimeDependency(
        "!com.eatthepath:fast-uuid:0.2.0",
        test = "!org.gitee.orryx.eatthepath.uuid.FastUUID",
        relocate = ["!com.eatthepath.uuid", "!org.gitee.orryx.eatthepath.uuid"],
        transitive = false
    ),
    RuntimeDependency(
        "!com.google.guava:guava:33.4.8-jre",
        test = "!org.gitee.orryx.google.common.base.Defaults",
        relocate = ["!com.google", "!org.gitee.orryx.google"],
        transitive = false
    ),
    RuntimeDependency(
        "!org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1",
        test = "!org.gitee.orryx.serialization.Serializer",
        relocate = ["!kotlin.", "!kotlin2120.", "!kotlinx.serialization.", "!org.gitee.orryx.serialization."],
        transitive = false
    ),
    RuntimeDependency(
        "!org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1",
        test = "!org.gitee.orryx.serialization.json.Json",
        relocate = ["!kotlin.", "!kotlin2120.", "!kotlinx.serialization.", "!org.gitee.orryx.serialization."],
        transitive = false
    )
)
class OrryxAPI: IOrryxAPI {

    override val keyAPI: IKeyAPI = PlatformFactory.getAPI<IKeyAPI>()

    override val reloadAPI: IReloadAPI = PlatformFactory.getAPI<IReloadAPI>()

    override val timerAPI: ITimerAPI = PlatformFactory.getAPI<ITimerAPI>()

    override val profileAPI: IProfileAPI = PlatformFactory.getAPI<IProfileAPI>()

    override val jobAPI: IJobAPI = PlatformFactory.getAPI<IJobAPI>()

    override val skillAPI: ISkillAPI = PlatformFactory.getAPI<ISkillAPI>()

    override val taskAPI: ITaskAPI = PlatformFactory.getAPI<ITaskAPI>()

    companion object {

        val ketherScriptLoader by lazy { KetherScriptLoader() }

        internal val saveScope = CoroutineScope(Dispatchers.async + SupervisorJob())
        internal val effectScope = CoroutineScope(Dispatchers.async + SupervisorJob())
        internal val pluginScope = CoroutineScope(Dispatchers.async + SupervisorJob())
    }
}