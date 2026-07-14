package org.gitee.orryx.core.reload

import com.germ.germplugin.api.event.GermReloadEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.server.ServerCommandEvent
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.events.OrryxPluginReloadEvent
import org.gitee.orryx.api.interfaces.IReloadAPI
import org.gitee.orryx.utils.consoleMessage
import org.gitee.orryx.utils.debug
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.Ghost
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.ClassMethod
import taboolib.library.reflex.ReflexClass

@Awake
object ReloadAPI: IReloadAPI, ClassVisitor(3) {

    class ReloadFunction(val method: ClassMethod, val obj: Any, val weight: Int)

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.ENABLE
    }

    private val methodList by unsafeLazy { mutableListOf<ReloadFunction>() }

    override fun visit(method: ClassMethod, owner: ReflexClass) {
        if (method.isAnnotationPresent(Reload::class.java)) {
            methodList += ReloadFunction(
                method,
                owner.getInstance() ?: return,
                method.getAnnotation(Reload::class.java).enum("weight")
            )
            debug { "&e┣&7Reload loaded &e${method.owner.name}/${method.name} &a√" }
        }
    }

    data class ReloadFailure(
        val phase: String,
        val target: String,
        val message: String,
    )

    data class ReloadReport(
        val success: Boolean,
        val cancelled: Boolean,
        val failures: List<ReloadFailure>,
    ) {
        fun summary(): String = when {
            cancelled -> "重载事件已取消"
            success -> "重载完成"
            else -> "重载失败: ${failures.joinToString("; ") { "${it.target}: ${it.message}" }}"
        }
    }

    override fun reload() {
        val report = reloadWithReport()
        if (!report.success) consoleMessage("&c[Orryx] ${report.summary()}")
    }

    /** 必须在 Bukkit 主线程调用，返回事件取消与每个重载函数的明确执行报告。 */
    fun reloadWithReport(): ReloadReport {
        val event = OrryxPluginReloadEvent()
        val eventAccepted = try {
            event.call()
        } catch (throwable: Throwable) {
            return ReloadReport(
                success = false,
                cancelled = false,
                failures = listOf(ReloadFailure("event", "OrryxPluginReloadEvent", failureMessage(throwable))),
            )
        }
        if (!eventAccepted) return ReloadReport(success = false, cancelled = true, failures = emptyList())

        consoleMessage("&e┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        val failures = mutableListOf<ReloadFailure>()
        try {
            Orryx.config.reload()
        } catch (throwable: Throwable) {
            failures += ReloadFailure("config", "config.yml", failureMessage(throwable))
        }
        if (failures.isEmpty()) {
            val extensions = event.getFunctions()
            val weights = (methodList.map { it.weight } + extensions.map { it.weight }).distinct()
            weights.sorted().forEach { weight ->
                methodList.asSequence().filter { it.weight == weight }.forEach { function ->
                    try {
                        function.method.invoke(function.obj)
                    } catch (throwable: Throwable) {
                        failures += ReloadFailure(
                            "annotated",
                            "${function.method.owner.name}/${function.method.name}",
                            failureMessage(throwable),
                        )
                    }
                }
                extensions.asSequence().filter { it.weight == weight }.forEachIndexed { index, function ->
                    try {
                        function.run()
                    } catch (throwable: Throwable) {
                        failures += ReloadFailure("extension", "weight=$weight,index=$index", failureMessage(throwable))
                    }
                }
            }
        }
        consoleMessage("&e┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        return ReloadReport(success = failures.isEmpty(), cancelled = false, failures = failures)
    }

    private fun failureMessage(throwable: Throwable): String {
        val cause = throwable.cause ?: throwable
        return cause.message ?: cause.javaClass.simpleName
    }

    @Awake(LifeCycle.CONST)
    fun init() {
        PlatformFactory.registerAPI<IReloadAPI>(ReloadAPI)
    }

    @Ghost
    @SubscribeEvent
    private fun reload(e: GermReloadEvent) {
        submit(delay = 1) {
            reload()
        }
    }

    @SubscribeEvent
    private fun reload(e: ServerCommandEvent) {
        if (e.command == "core reload" || e.command == "dragoncore reload") {
            submit(delay = 1) {
                reload()
            }
        }
    }

    @SubscribeEvent
    private fun reload(e: PlayerCommandPreprocessEvent) {
        if (e.message == "/core reload" || e.message == "/dragoncore reload") {
            if (e.player.isOp) {
                submit(delay = 1) {
                    reload()
                }
            }
        }
    }
}