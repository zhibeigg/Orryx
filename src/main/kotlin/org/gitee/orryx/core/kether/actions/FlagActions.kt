package org.gitee.orryx.core.kether.actions

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.profile.PlayerProfileManager.orryxProfile
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object FlagActions {

    @KetherParser(["flag"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionFlag() = scriptParser(
        arrayOf(
            Action.new("Flag数据标签", "获取数据标签", "flag", true)
                .description("获取数据标签")
                .addEntry("获取占位符", Type.SYMBOL, true, head = "get", default = "get")
                .addEntry("键名", Type.STRING, false)
                .addContainerEntry("获取的玩家", true, "@self")
                .result("数据", Type.ANY),
            Action.new("Flag数据标签", "创建数据", "flag", true)
                .description("创建一个存储任意类型数据的标签，可持久化存储向量,矩阵,Bukkit实体,Ady实体,时间,和所有基础类型")
                .addEntry("创建占位符", Type.SYMBOL, false, head = "create/to")
                .addEntry("键名", Type.STRING, false)
                .addEntry("数据", Type.ANY, false)
                .addEntry("是否持久化", Type.BOOLEAN, default = "false", head = "pst")
                .addEntry("存活时长，默认永久", Type.LONG, default = "0", head = "timeout")
                .addContainerEntry("创建的玩家", true, "@self")
                .result("数据", Type.ANY),
            Action.new("Flag数据标签", "删除数据标签", "flag", true)
                .description("删除数据标签")
                .addEntry("删除占位符", Type.SYMBOL, head = "remove/delete")
                .addEntry("键名", Type.STRING, false)
                .addContainerEntry("删除的玩家", true, "@self")
                .result("数据", Type.ANY),
            Action.new("Flag数据标签", "清除所有数据标签", "flag", true)
                .description("清除所有数据标签")
                .addEntry("清除占位符", Type.SYMBOL, head = "clear")
                .addContainerEntry("清除的玩家", true, "@self"),
            Action.new("Flag数据标签", "获取数据存活时间", "flag", true)
                .description("获取数据存活时间")
                .addEntry("存活占位符", Type.SYMBOL, head = "survival")
                .addEntry("键名", Type.STRING, false)
                .addContainerEntry("获取的玩家", true, "@self")
                .result("数据存活时间(Tick)", Type.LONG),
            Action.new("Flag数据标签", "获取数据剩余存活时间", "flag", true)
                .description("获取数据剩余存活时间")
                .addEntry("剩余存活占位符", Type.SYMBOL, head = "countdown")
                .addEntry("键名", Type.STRING, false)
                .addContainerEntry("获取的玩家", true, "@self")
                .result("数据剩余存活时间(Tick)", Type.LONG)
        )
    ) {
        it.switch {
            case("get") {
                get(it)
            }
            case("create") {
                create(it)
            }
            case("remove") {
                remove(it)
            }
            case("clear") {
                clear(it)
            }
            case("survival") {
                survival(it)
            }
            case("countdown") {
                countdown(it)
            }
            other { get(it) }
        }
    }

    private fun get(reader: QuestReader): ScriptAction<Any?> {
        val key = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrSelf()
        return actionFuture { future ->
            run(key).str { key ->
                container(they, self()) {
                    future.complete(it.firstInstance<ITargetEntity<Player>>().getSource().orryxProfile().getFlag(key))
                }
            }
        }
    }

    private fun create(reader: QuestReader): ScriptAction<Any?> {
        val key = reader.nextParsedAction()
        val value = reader.nextParsedAction()
        val persistence = reader.nextHeadAction("pst", false)
        val timeout = reader.nextHeadAction("timeout", 0)
        val they = reader.nextTheyContainerOrSelf()
        return actionFuture { future ->
            run(key).str { key ->
                run(value).thenAccept { value ->
                    run(persistence).bool { persistence ->
                        run(timeout).long { timeout ->
                            container(they, self()) {
                                it.forEachInstance<ITargetEntity<Player>> {
                                    value?.flag(persistence, timeout)?.let { it1 -> it.getSource().orryxProfile().setFlag(key, it1) }
                                }
                                future.complete(value)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun remove(reader: QuestReader): ScriptAction<Any?> {
        val key = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrSelf()
        return actionFuture { future ->
            run(key).str { key ->
                container(they, self()) {
                    future.complete(it.firstInstance<ITargetEntity<Player>>().getSource().orryxProfile().removeFlag(key))
                }
            }
        }
    }

    private fun clear(reader: QuestReader): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrSelf()
        return actionNow {
            container(they, self()) {
                it.forEachInstance<ITargetEntity<Player>> { target ->
                    target.getSource().orryxProfile().clearFlags()
                }
            }
        }
    }

    private fun survival(reader: QuestReader): ScriptAction<Any?> {
        val key = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrSelf()
        return actionFuture { future ->
            run(key).str { key ->
                container(they, self()) {
                    future.complete(it.firstInstance<ITargetEntity<Player>>().getSource().orryxProfile().getFlag(key)?.timestamp?.let { timestamp ->
                        System.currentTimeMillis() - timestamp
                    } ?: 0)
                }
            }
        }
    }

    private fun countdown(reader: QuestReader): ScriptAction<Any?> {
        val key = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrSelf()
        return actionFuture { future ->
            run(key).str { key ->
                container(they, self()) {
                    val flag = it.firstInstance<ITargetEntity<Player>>().getSource().orryxProfile().getFlag(key)
                    future.complete(flag?.let {
                        flag.timestamp + flag.timeout - System.currentTimeMillis()
                    } ?: 0)
                }
            }
        }
    }




}