package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.addOrryxCloseable
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.presets.SelectorPresetsLoaderManager
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.readContainer
import org.gitee.orryx.utils.runSubScript
import org.gitee.orryx.utils.scriptParser
import taboolib.common.platform.function.submitAsync
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object SelectorActions {

    @KetherParser(["selector"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionSelector() = scriptParser(
        Action.new("Selector选择器", "载入预设", "selector", true)
            .description("载入配置文件中的预设选择器")
            .addEntry("预设占位符", Type.SYMBOL, true, default = "preset", head = "preset")
            .addEntry("预设键名", Type.STRING, false),
        Action.new("Selector选择器", "显示选区", "selector", true)
            .description("用粒子显示包含的几何Selector的选区")
            .addEntry("显示占位符", Type.SYMBOL, head = "show")
            .addEntry("显示时长", Type.LONG)
            .addEntry("显示的选择器（只支持几何选择器）", Type.STRING, false, head = "they")
    ) {
        it.switch {
            case("preset") {
                preset(it)
            }
            case("show") {
                show(it)
            }
            other { preset(it) }
        }
    }

    private fun preset(reader: QuestReader): ScriptAction<Any?> {
        val key = reader.nextParsedAction()
        return actionFuture { future ->
            run(key).str { key ->
                SelectorPresetsLoaderManager.getSelectorPreset(key)?.action?.let { action ->
                    script().runSubScript(
                        action,
                        true
                    )
                }?.thenAccept {
                    it.readContainer(script())
                } ?: future.complete(null)
            }
        }
    }

    private fun show(reader: QuestReader): ScriptAction<Any?> {
        val timeout = reader.nextParsedAction()
        reader.expects("they")
        val they = reader.nextParsedAction()
        return actionFuture { future ->
            run(timeout).long { timeout ->
                run(they).str { they ->
                    var time = 0
                    val task = submitAsync(period = 5) {
                        if (time * 5 >= timeout) {
                            future.complete(null)
                            cancel()
                        }
                        StringParser(they).showAFrame(script())
                        time++
                    }
                    addOrryxCloseable(future) { task.cancel() }
                }
            }
        }
    }
}