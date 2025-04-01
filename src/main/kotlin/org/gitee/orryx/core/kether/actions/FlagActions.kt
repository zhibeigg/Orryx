package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.library.kether.LocalizedException
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object FlagActions {

    @KetherParser(["flag"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionFlag() = scriptParser(
        arrayOf(
            Action.new("Flag数据标签", "获取数据标签", "flag", true)
                .description("获取数据标签")
                .addEntry("键名", Type.STRING, false)
                .addContainerEntry("获取的玩家", true, "@self")
                .result("数据", Type.ANY),
            Action.new("Flag数据标签", "创建数据", "flag", true)
                .description("创建一个存储任意类型数据的标签，可持久化存储向量,矩阵,Bukkit实体,Ady实体,时间,和所有基础类型")
                .addEntry("键名", Type.STRING, false)
                .addEntry("创建占位符", Type.SYMBOL, false, head = "set/to")
                .addEntry("数据", Type.ANY, false)
                .addEntry("是否持久化，默认false", Type.BOOLEAN, true, default = "false", head = "pst")
                .addEntry("存活时长，默认永久", Type.LONG, true, default = "0", head = "timeout")
                .addContainerEntry("创建的玩家", true, "@self")
                .result("数据", Type.ANY),
            Action.new("Flag数据标签", "删除数据标签", "flag", true)
                .description("删除数据标签")
                .addEntry("键名", Type.STRING, false)
                .addEntry("删除占位符", Type.SYMBOL, head = "remove/delete")
                .addContainerEntry("删除的玩家", true, "@self")
                .result("数据", Type.ANY),
            Action.new("Flag数据标签", "清除所有数据标签", "flag", true)
                .description("清除所有数据标签")
                .addEntry("清除占位符", Type.SYMBOL, head = "clear")
                .addContainerEntry("清除的玩家", true, "@self"),
            Action.new("Flag数据标签", "获取数据存活时间", "flag", true)
                .description("获取数据存活时间")
                .addEntry("键名", Type.STRING, false)
                .addEntry("存活占位符", Type.SYMBOL, head = "survival")
                .addContainerEntry("获取的玩家", true, "@self")
                .result("数据存活时间(Tick)", Type.LONG),
            Action.new("Flag数据标签", "获取数据剩余存活时间", "flag", true)
                .description("获取数据剩余存活时间")
                .addEntry("键名", Type.STRING, false)
                .addEntry("剩余存活占位符", Type.SYMBOL, head = "countdown")
                .addContainerEntry("获取的玩家", true, "@self")
                .result("数据剩余存活时间(Tick)", Type.LONG)
        )
    ) {
        try {
            it.mark()
            when(it.expects("clear")) {
                "clear" -> clear(it)
            }
        } catch(e: LocalizedException) {
            it.reset()
        }
        val key = it.nextParsedAction()
        it.switch {
            case("remove", "delete") { remove(it, key) }
            case("survival") { survival(it, key) }
            case("countdown") { countdown(it, key) }
            other { setOrGet(it, key) }
        }
    }

    private fun setOrGet(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {
        try {
            reader.mark()
            reader.expects("to", "set")
            val value = reader.nextParsedAction()
            val persistence = reader.nextHeadAction("pst", false)
            val timeout = reader.nextHeadAction("timeout", 0)
            val they = reader.nextTheyContainerOrNull()

            return actionNow {
                run(key).str { key ->
                    run(value).thenAccept { value ->
                        run(persistence).bool { persistence ->
                            run(timeout).long { timeout ->
                                containerOrSelf(they) {
                                    it.forEachInstance<PlayerTarget> { target ->
                                        target.getSource().orryxProfile { profile ->
                                            value?.flag(persistence, timeout*50)?.let { it1 -> profile.setFlag(key, it1) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: LocalizedException) {
            reader.reset()
        }
        val they = reader.nextTheyContainerOrNull()

        return actionFuture { future ->
            run(key).str { key ->
                containerOrSelf(they) {
                    it.firstInstance<PlayerTarget>().getSource().orryxProfile { profile ->
                        future.complete(profile.getFlag(key)?.value)
                    }
                }
            }
        }
    }

    private fun remove(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()

        return actionFuture { future ->
            run(key).str { key ->
                containerOrSelf(they) {
                    it.firstInstance<PlayerTarget>().getSource().orryxProfile { profile ->
                        future.complete(profile.removeFlag(key))
                    }
                }
            }
        }
    }

    private fun clear(reader: QuestReader): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            containerOrSelf(they) {
                it.forEachInstance<PlayerTarget> { target ->
                    target.getSource().orryxProfile { profile ->
                        profile.clearFlags()
                    }
                }
            }
        }
    }

    private fun survival(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()

        return actionFuture { future ->
            run(key).str { key ->
                containerOrSelf(they) {
                    it.firstInstance<PlayerTarget>().getSource().orryxProfile { profile ->
                        val flag = profile.getFlag(key)

                        future.complete(
                            flag?.let {
                                (System.currentTimeMillis() - flag.timestamp) / 50
                            } ?: 0
                        )
                    }
                }
            }
        }
    }

    private fun countdown(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()

        return actionFuture { future ->
            run(key).str { key ->
                containerOrSelf(they) {
                    it.firstInstance<PlayerTarget>().getSource().orryxProfile { profile ->
                        val flag = profile.getFlag(key)

                        future.complete(
                            flag?.let {
                                (flag.timestamp + flag.timeout - System.currentTimeMillis()) / 50
                            } ?: 0
                        )
                    }
                }
            }
        }
    }




}