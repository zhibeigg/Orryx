package org.gitee.orryx.core.kether.actions

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.library.kether.LocalizedException
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

object FlagActions {

    @KetherParser(["flag"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionFlag() = scriptParser(
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
    ) {
        try {
            it.mark()
            if (it.expects("clear") == "clear") {
                @Suppress("UNCHECKED_CAST")
                return@scriptParser clear(it) as taboolib.library.kether.QuestAction<Any?>
            }
        } catch (_: LocalizedException) {
            it.reset()
        }
        val key = it.nextParsedAction()
        @Suppress("UNCHECKED_CAST")
        (it.switch {
            case("remove", "delete") { remove(it, key) }
            case("survival") { survival(it, key) }
            case("countdown") { countdown(it, key) }
            other { setOrGet(it, key) }
        } as taboolib.library.kether.QuestAction<Any?>)
    }

    private fun setOrGet(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {
        try {
            reader.mark()
            reader.expects("to", "set")
            val value = reader.nextParsedAction()
            val persistence = reader.nextHeadAction("pst", def = false)
            val timeout = reader.nextHeadAction("timeout", def = 0)
            val they = reader.nextTheyContainerOrNull()

            return actionFuture { future ->
                run(key).str { it }.thenCompose { flagKey ->
                    run(value).thenCompose { flagValue ->
                        run(persistence).bool { it }.thenCompose { persistent ->
                            run(timeout).long { it }.thenCompose { ticks ->
                                val created = flagValue?.flag(persistent, ticksToMillisSaturated(ticks))
                                    ?: return@thenCompose CompletableFuture.completedFuture(null)
                                profiles(they).thenCompose { profiles ->
                                    val operations = profiles.map { it.setFlagFuture(flagKey, created) }
                                    CompletableFuture.allOf(*operations.toTypedArray()).thenApply { flagValue }
                                }
                            }
                        }
                    }
                }.completeInto(future)
            }
        } catch (_: LocalizedException) {
            reader.reset()
        }
        val they = reader.nextTheyContainerOrNull()

        return actionFuture { future ->
            run(key).str { it }.thenCompose { flagKey ->
                profiles(they).thenApply { profiles -> profiles.firstOrNull()?.getFlag(flagKey)?.value }
            }.completeInto(future)
        }
    }

    private fun remove(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()

        return actionFuture { future ->
            run(key).str { it }.thenCompose { flagKey ->
                profiles(they).thenCompose { profiles ->
                    profiles.firstOrNull()?.removeFlagFuture(flagKey)
                        ?: CompletableFuture.completedFuture(null)
                }
            }.completeInto(future)
        }
    }

    private fun clear(reader: QuestReader): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()

        return actionFuture { future ->
            profiles(they).thenCompose { profiles ->
                val operations = profiles.map { it.clearFlagsFuture() }
                CompletableFuture.allOf(*operations.toTypedArray()).thenApply { Unit }
            }.completeInto(future)
        }
    }

    private fun survival(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()

        return actionFuture { future ->
            run(key).str { it }.thenCompose { flagKey ->
                profiles(they).thenApply { profiles ->
                    profiles.firstOrNull()?.getFlag(flagKey)?.let {
                        (System.currentTimeMillis() - it.timestamp).coerceAtLeast(0L) / 50L
                    } ?: 0L
                }
            }.completeInto(future)
        }
    }

    private fun countdown(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()

        return actionFuture { future ->
            run(key).str { it }.thenCompose { flagKey ->
                profiles(they).thenApply { profiles ->
                    profiles.firstOrNull()?.getFlag(flagKey)?.let {
                        if (it.expiresAt == 0L) 0L else positiveDifference(it.expiresAt, System.currentTimeMillis()) / 50L
                    } ?: 0L
                }
            }.completeInto(future)
        }
    }

    private fun ScriptFrame.profiles(container: ParsedAction<*>?): CompletableFuture<List<IPlayerProfile>> {
        return containerOrSelf(container) { targets ->
            targets.mapInstance<PlayerTarget, Player> { it.getSource() }
        }.thenCompose { players ->
            val loads = players.map { it.orryxProfile().thenApply<IPlayerProfile?> { profile -> profile } }
            CompletableFuture.allOf(*loads.toTypedArray()).thenApply {
                loads.mapNotNull { it.getNow(null) }
            }
        }
    }

    private fun positiveDifference(end: Long, start: Long): Long {
        if (end <= start) return 0L
        return if (start < 0L && end > Long.MAX_VALUE + start) Long.MAX_VALUE else end - start
    }
}