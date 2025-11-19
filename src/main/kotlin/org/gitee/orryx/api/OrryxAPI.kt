package org.gitee.orryx.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.gitee.orryx.api.interfaces.*
import org.gitee.orryx.utils.minecraftAsync
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
        "!com.larksuite.oapi:oapi-sdk:2.4.22",
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
    ),
    RuntimeDependency(
        "!org.jetbrains.kotlin:kotlin-scripting-common:2.1.20",
        test = "!kotlin2120.script.experimental.api.ScriptEvaluator",
        relocate = ["!kotlin.", "!kotlin2120."],
        transitive = false
    ),
    RuntimeDependency(
        "!org.jetbrains.kotlin:kotlin-scripting-jvm:2.1.20",
        test = "!kotlin2120.script.experimental.jvm.JvmDependency",
        relocate = ["!kotlin.", "!kotlin2120."],
        transitive = false
    ),
    RuntimeDependency(
        "!org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.1.20",
        test = "!kotlin2120.script.experimental.jvmhost.JvmScriptCompiler",
        relocate = ["!kotlin.", "!kotlin2120."],
        transitive = false
    ),
    RuntimeDependency(
        "!org.jetbrains.kotlin:kotlin-scripting-dependencies:2.1.20",
        test = "!kotlin2120.script.experimental.dependencies.DependsOn",
        relocate = ["!kotlin.", "!kotlin2120."],
        transitive = false
    ),
    RuntimeDependency(
        "!org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:2.1.20",
        test = "!kotlin2120.script.experimental.dependencies.maven.MavenDependenciesResolver",
        relocate = ["!kotlin.", "!kotlin2120."],
        transitive = false
    ),
    RuntimeDependency(
        "!org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.20",
        test = "!org.jetbrains.kotlin.compilerRunner.CompilerEnvironment",
        relocate = ["!kotlin.", "!kotlin2120."],
        transitive = false
    ),
    RuntimeDependency(
        "!org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:2.1.20",
        test = "!org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys",
        relocate = ["!kotlin.", "!kotlin2120."],
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

    override val consumptionValueAPI: IConsumptionValueAPI = PlatformFactory.getAPI<IConsumptionValueAPI>()

    override val miscAPI: IMiscAPI = PlatformFactory.getAPI<IMiscAPI>()

    companion object {

        val ketherScriptLoader by lazy { KetherScriptLoader() }

        internal val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        internal val effectScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        internal val pluginScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }
}