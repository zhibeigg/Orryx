package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.addOrryxCloseable
import org.gitee.orryx.core.station.pipe.PipeBuilder
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.combinationParser
import org.gitee.orryx.utils.parseUUID
import taboolib.library.kether.ParsedAction
import taboolib.module.kether.KetherParser
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.run
import taboolib.module.kether.script
import java.util.concurrent.CompletableFuture

object PipeActions {

    /**
     * # 管式任务
     * 阻塞kether语句直到执行完毕
     *```
     * tell pipe <uuid: UUID> <timeout: Long> trigger <String> onComplete {
     *   tell "随便运行最后一行返回值"
     *   1
     * } onBrock {
     *   tell "随便运行最后一行返回值"
     *   2
     * } onPeriod {
     *   break
     *   tell "tick"
     * } period 5
     * ```
     * */
    @KetherParser(["pipe"], shared = true)
    private fun pipe() = combinationParser(
        Action.new("Pipe管式任务", "管式任务", "pipe", true)
            .description("阻塞kether语句直到执行完毕")
            .addEntry("UUID，唯一ID名", Type.STRING)
            .addEntry("多少Ticks后完成", Type.LONG)
            .addEntry("中断触发器使用 , 分割Key", Type.STRING, optional = true, head = "trigger")
            .addEntry("完成时运行脚本 { action }", Type.ANY, optional = true, head = "onComplete")
            .addEntry("中断时运行脚本 { action }", Type.ANY, optional = true, head = "onBrock")
            .addEntry("每周期 tick 时运行脚本 { action }", Type.ANY, optional = true, head = "onPeriod")
            .addEntry("周期Tick", Type.LONG, optional = true, head = "period")
            .result("完成或中断运行脚本返回值", Type.ANY)
    ) {
        it.group(
            text(),
            long(),
            command("trigger", then = text()).option(),
            command("onComplete", then = action()).option(),
            command("onBrock", then = action()).option(),
            command("onPeriod", then = action()).option(),
            command("period", then = long()).option()
        ).apply(it) { uuid, timeout, triggers, onComplete, onBrock, onPeriod, period ->
            future {
                val pipe = PipeBuilder()
                    .uuid(uuid.parseUUID()!!)
                    .scriptContext(script())
                    .timeout(timeout)
                if (onComplete != null) {
                    pipe.onComplete {
                        run(onComplete)
                    }
                }
                if (onBrock != null) {
                    pipe.onBrock {
                        run(onBrock)
                    }
                }
                if (triggers != null) {
                    pipe.brokeTriggers(*triggers.uppercase().split(",").toTypedArray())
                }
                if (onPeriod != null) {
                    pipe.periodTaskAsync(period ?: 1) {
                        runCancellable(onPeriod)
                    }
                }
                val task = pipe.build()
                addOrryxCloseable(task.result) { task.abort() }
                task.result
            }
        }
    }

    private fun ScriptFrame.runCancellable(action: ParsedAction<*>): CompletableFuture<Any?> {
        val child = newFrame(action)
        val execution = child.run<Any?>()
        val result = CompletableFuture<Any?>()
        result.whenComplete { _, _ ->
            if (result.isCancelled) {
                execution.cancel(false)
                runCatching { child.close() }.onFailure(Throwable::printStackTrace)
            }
        }
        execution.whenComplete { value, throwable ->
            val closeFailure = runCatching { child.close() }.exceptionOrNull()
            val failure = throwable ?: closeFailure
            if (failure == null) {
                result.complete(value)
            } else {
                if (throwable != null && closeFailure != null && throwable !== closeFailure) {
                    throwable.addSuppressed(closeFailure)
                }
                result.completeExceptionally(failure)
            }
        }
        return result
    }
}