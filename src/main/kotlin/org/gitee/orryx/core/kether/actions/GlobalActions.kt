package org.gitee.orryx.core.kether.actions

import kotlinx.coroutines.launch
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.OrryxGlobalFlagChangeEvents
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.flag
import org.gitee.orryx.utils.nextHeadAction
import org.gitee.orryx.utils.scriptParser
import taboolib.common.platform.function.isPrimaryThread
import taboolib.library.kether.LocalizedException
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object GlobalActions {

    private val globalFlagMap = ConcurrentHashMap<String, IFlag>()

    private fun persistGlobalFlag(flagName: String, flag: IFlag?, callback: Runnable = Runnable { }) {
        val saveTask = {
            try {
                IStorageManager.INSTANCE.saveGlobalFlag(flagName, flag, callback)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
        if (isPrimaryThread && !GameManager.shutdown) {
            OrryxAPI.ioScope.launch { saveTask() }
        } else {
            saveTask()
        }
    }

    private fun getFlagAsync(flagName: String): CompletableFuture<IFlag?> {
        globalFlagMap[flagName]?.let {
            if (it.isTimeout()) {
                globalFlagMap.remove(flagName)
                persistGlobalFlag(flagName, null)
                return CompletableFuture.completedFuture(null)
            }
            return CompletableFuture.completedFuture(it)
        }
        return IStorageManager.INSTANCE.getGlobalFlag(flagName).thenApply { loaded ->
            when {
                loaded == null -> null
                loaded.isTimeout() -> {
                    globalFlagMap.remove(flagName)
                    persistGlobalFlag(flagName, null)
                    null
                }
                else -> {
                    globalFlagMap.putIfAbsent(flagName, loaded)
                    globalFlagMap[flagName]
                }
            }
        }
    }

    @KetherParser(["global"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionGlobal() = scriptParser(
        Action.new("Global全局数据标签", "获取数据标签", "global", true)
            .description("获取数据标签")
            .addEntry("键名", Type.STRING, false)
            .result("数据", Type.ANY),
        Action.new("Global全局数据标签", "创建数据", "global", true)
            .description("创建一个存储任意类型数据的标签，可持久化存储向量,矩阵,Bukkit实体,Ady实体,时间,和所有基础类型")
            .addEntry("键名", Type.STRING, false)
            .addEntry("创建占位符", Type.SYMBOL, false, head = "set/to")
            .addEntry("数据", Type.ANY, false)
            .addEntry("是否持久化，默认false", Type.BOOLEAN, true, default = "false", head = "pst")
            .addEntry("存活时长，默认永久", Type.LONG, true, default = "0", head = "timeout")
            .result("数据", Type.ANY),
        Action.new("Global全局数据标签", "删除数据标签", "global", true)
            .description("删除数据标签")
            .addEntry("键名", Type.STRING, false)
            .addEntry("删除占位符", Type.SYMBOL, head = "remove/delete")
            .result("数据", Type.ANY),
        Action.new("Global全局数据标签", "清除所有数据标签", "global", true)
            .description("清除所有数据标签")
            .addEntry("清除占位符", Type.SYMBOL, head = "clear"),
        Action.new("Global全局数据标签", "获取数据存活时间", "global", true)
            .description("获取数据存活时间")
            .addEntry("键名", Type.STRING, false)
            .addEntry("存活占位符", Type.SYMBOL, head = "survival")
            .result("数据存活时间(Tick)", Type.LONG),
        Action.new("Global全局数据标签", "获取数据剩余存活时间", "global", true)
            .description("获取数据剩余存活时间")
            .addEntry("键名", Type.STRING, false)
            .addEntry("剩余存活占位符", Type.SYMBOL, head = "countdown")
            .result("数据剩余存活时间(Tick)", Type.LONG)
    ) {
        try {
            it.mark()
            when (it.expects("clear")) {
                "clear" -> clear(it)
            }
        } catch (_: LocalizedException) {
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
            val persistence = reader.nextHeadAction("pst", def = false)
            val timeout = reader.nextHeadAction("timeout", def = 0)

            return actionNow {
                run(key).str { key ->
                    run(value).thenAccept { value ->
                        run(persistence).bool { persistence ->
                            run(timeout).long { timeout ->
                                value?.flag(persistence, timeout * 50)?.let { it1 -> setFlag(key, it1) }
                            }
                        }
                    }
                }
            }
        } catch (_: LocalizedException) {
            reader.reset()
        }

        return actionFuture { future ->
            run(key).str { key ->
                getFlagAsync(key).whenComplete { flag, error ->
                    if (error != null) {
                        future.completeExceptionally(error)
                    } else {
                        future.complete(flag?.value)
                    }
                }
            }
        }
    }

    private fun remove(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {

        return actionFuture { future ->
            run(key).str { key ->
                getFlagAsync(key).whenComplete { _, error ->
                    if (error != null) {
                        future.completeExceptionally(error)
                    } else {
                        future.complete(removeFlag(key))
                    }
                }
            }
        }

    }

    private fun clear(reader: QuestReader): ScriptAction<Any?> {

        return actionNow {
            clearFlags()
        }
    }

    private fun survival(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {

        return actionFuture { future ->
            run(key).str { key ->
                getFlagAsync(key).whenComplete { flag, error ->
                    if (error != null) {
                        future.completeExceptionally(error)
                    } else {
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

        return actionFuture { future ->
            run(key).str { key ->
                getFlagAsync(key).whenComplete { flag, error ->
                    if (error != null) {
                        future.completeExceptionally(error)
                    } else {
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

    /**
     * 尝试获取flag
     * @param flagName flag的键名
     * @return flag
     * */
    fun getFlag(flagName: String): IFlag? {
        return globalFlagMap[flagName]
    }

    /**
     * 设置flag
     * @param flagName flag的键名
     * @param flag flag
     * @param save 是否检测是否持久并保存
     * */
    fun setFlag(flagName: String, flag: IFlag, save: Boolean = true) {
        val event = OrryxGlobalFlagChangeEvents.Pre(flagName, globalFlagMap[flagName], flag)

        if (event.call()) {
            if (event.newFlag == null) {
                globalFlagMap.remove(flagName)
            } else {
                globalFlagMap[flagName] = event.newFlag!!
            }
            if (save && (event.oldFlag?.isPersistence == true || event.newFlag?.isPersistence == true)) {
                persistGlobalFlag(flagName, event.newFlag) {
                    OrryxGlobalFlagChangeEvents.Post(flagName, event.oldFlag, event.newFlag).call()
                }
            }
        }
    }

    /**
     * 移除flag
     * @param flagName flag的键名
     * @param save 是否检测是否持久并保存
     * @return 移除的flag
     * */
    fun removeFlag(flagName: String, save: Boolean = true): IFlag? {
        val event = OrryxGlobalFlagChangeEvents.Pre(flagName, globalFlagMap[flagName], null)

        if (event.call()) {
            if (event.newFlag == null) {
                globalFlagMap.remove(flagName)
            } else {
                globalFlagMap[flagName] = event.newFlag!!
            }
            if (save && (event.oldFlag?.isPersistence == true || event.newFlag?.isPersistence == true)) {
                persistGlobalFlag(flagName, event.newFlag) {
                    OrryxGlobalFlagChangeEvents.Post(flagName, event.oldFlag, event.newFlag).call()
                }
            }
        }
        return event.oldFlag
    }

    /**
     * 清除flag
     * */
    fun clearFlags() {
        globalFlagMap.clear()
    }
}
