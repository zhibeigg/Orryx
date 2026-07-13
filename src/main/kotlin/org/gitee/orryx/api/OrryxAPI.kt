package org.gitee.orryx.api

import kotlinx.coroutines.*
import org.gitee.orryx.api.interfaces.*
import org.gitee.orryx.utils.minecraftMain
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.warning
import taboolib.module.kether.KetherScriptLoader
import kotlin.time.Duration.Companion.seconds

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
        "!org.java-websocket:Java-WebSocket:1.5.7",
        test = "!org.gitee.orryx.java_websocket.client.WebSocketClient",
        relocate = ["!org.java_websocket.", "!org.gitee.orryx.java_websocket."],
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

        /**
         * 协程异常处理器，捕获未处理的异常并记录日志
         */
        private val exceptionHandler by lazy {
            CoroutineExceptionHandler { context, throwable ->
                val coroutineName = context[CoroutineName]?.name ?: "unknown"
                warning("[Orryx] 协程 '$coroutineName' 发生未捕获异常: ${throwable.message}")
                if (throwable !is CancellationException) {
                    throwable.printStackTrace()
                }
            }
        }

        private val ioJob by lazy { SupervisorJob() }
        private val effectJob by lazy { SupervisorJob() }
        private val pluginJob by lazy { SupervisorJob() }

        internal val ioScope by lazy { CoroutineScope(Dispatchers.IO + ioJob + exceptionHandler + CoroutineName("orryx-io")) }
        internal val effectScope by lazy { CoroutineScope(Dispatchers.Default + effectJob + exceptionHandler + CoroutineName("orryx-effect")) }
        internal val pluginScope by lazy { CoroutineScope(Dispatchers.minecraftMain + pluginJob + exceptionHandler + CoroutineName("orryx-plugin")) }

        /**
         * 优雅关闭所有协程作用域
         * @param timeout 等待协程完成的超时时间（秒）
         */
        internal fun shutdownScopes(@Suppress("UNUSED_PARAMETER") timeout: Long = 5) {
            // 生命周期关闭线程绝不等待 Future/协程；持久化和连接关闭在独立 runtime 完成后调用这里。
            pluginJob.cancel()
            effectJob.cancel()
            ioJob.cancel()
        }
    }
}