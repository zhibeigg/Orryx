package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.kether.ScriptManager.addOrryxCloseable
import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submit
import taboolib.common5.clong
import taboolib.module.kether.KetherParser
import taboolib.module.kether.actionFuture
import taboolib.module.kether.long
import taboolib.module.kether.run

object Actions {

    @KetherParser(["wait", "delay", "sleep"], namespace = ORRYX_NAMESPACE)
    private fun actionWait() = scriptParser(
        arrayOf(
            Action.new("普通语句", "延迟delay", "wait/delay/sleep")
                .description("延迟多少Tick")
                .addEntry("tick", Type.LONG)
        )
    ) {
        val ticks = it.nextParsedAction()
        actionFuture { f ->
            run(ticks).long { ticks ->
                val task = submit(delay = ticks, async = !isPrimaryThread) {
                    f.complete(null)
                }
                addOrryxCloseable(f) { task.cancel() }
            }
        }
    }

    @KetherParser(["sync"], namespace = ORRYX_NAMESPACE)
    private fun actionSync() = scriptParser(
        arrayOf(
            Action.new("普通语句", "同步Sync", "sync")
                .description("将语句在主线程运行并等待返回")
                .addEntry("actions", Type.ANY)
        )
    ) {
        val action = it.nextParsedAction()
        actionFuture {future ->
            ensureSync { run(action).thenAccept { value -> future.complete(value) } }
        }
    }

    @KetherParser(["silence"], namespace = ORRYX_NAMESPACE)
    private fun actionSilence() = combinationParser(
        Action.new("普通语句", "沉默玩家", "silence")
            .description("沉默中无法释放任何主动技能")
            .addEntry("沉默时间", Type.LONG)
            .addContainerEntry("沉默的玩家", true, "@self")
    ) {
        it.group(
            long(),
            theyContainer(true)
        ).apply(it) { timeout, they ->
            future {
                ensureSync {
                    they.orElse(self()).forEachInstance<PlayerTarget> { target ->
                        silence(adaptPlayer(target.getSource()), timeout * 50)
                    }
                }
            }
        }
    }

    @KetherParser(["silenced"], namespace = ORRYX_NAMESPACE)
    private fun actionIsSilenced() = combinationParser(
        Action.new("普通语句", "玩家是否在沉默中", "silenced")
            .description("检测玩家是否在沉默中")
            .addContainerEntry("检测的玩家", true, "@self")
            .result("是否沉默", Type.BOOLEAN)
    ) {
        it.group(
            theyContainer(true)
        ).apply(it) { they ->
            now {
                they.orElse(self()).firstInstanceOrNull<PlayerTarget>()?.getSource()?.let { player ->
                    SkillTimer.hasNext(player, SILENCE_TAG)
                } ?: false
            }
        }
    }

    @KetherParser(["silenceTime"], namespace = ORRYX_NAMESPACE)
    private fun actionSilenceTime() = combinationParser(
        Action.new("普通语句", "玩家剩余的沉默时间", "silenceTime")
            .description("玩家剩余的沉默时间")
            .addContainerEntry("检测的玩家", true, "@self")
            .result("玩家剩余的沉默时间", Type.LONG)
    ) {
        it.group(
            theyContainer(true)
        ).apply(it) { they ->
            now {
                they.orElse(self()).firstInstanceOrNull<PlayerTarget>()?.getSource()?.let { player ->
                    (SkillTimer.getCountdown(player, SILENCE_TAG) / 50).clong
                } ?: 0L
            }
        }
    }

}