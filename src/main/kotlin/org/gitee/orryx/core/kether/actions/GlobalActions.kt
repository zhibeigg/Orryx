package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.api.events.OrryxGlobalFlagChangeEvents
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.dao.persistence.PersistenceManager
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.library.kether.LocalizedException
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object GlobalActions {

    private val globalFlagMap = ConcurrentHashMap<String, IFlag>()
    private val mutationLock = Any()
    private var mutationTail = CompletableFuture.completedFuture(Unit)
    private var mutationFailure: Throwable? = null
    private var acceptingMutations = true
    private const val MAX_CACHE_SIZE = 10000

    private fun <T> enqueueMutation(operation: () -> CompletableFuture<T>): CompletableFuture<T> {
        val result: CompletableFuture<T>
        synchronized(mutationLock) {
            if (!acceptingMutations) {
                return failedFuture(IllegalStateException("Orryx 全局 Flag 队列正在关闭"))
            }
            val ready = mutationTail.handle { _, _ -> Unit }
            result = ready.thenCompose {
                try {
                    operation()
                } catch (throwable: Throwable) {
                    failedFuture(throwable)
                }
            }
            mutationTail = result.handle { _, throwable ->
                if (throwable != null) synchronized(mutationLock) {
                    if (mutationFailure == null) mutationFailure = throwable
                }
                Unit
            }
        }
        return result
    }

    internal fun shutdown(): CompletableFuture<Unit> {
        val tail = synchronized(mutationLock) {
            acceptingMutations = false
            mutationTail
        }
        return tail.thenCompose {
            synchronized(mutationLock) { mutationFailure }?.let(::failedFuture)
                ?: CompletableFuture.completedFuture(Unit)
        }
    }

    private fun persistGlobalFlags(
        changes: Map<String, IFlag?>,
        clearAll: Boolean = false,
    ): CompletableFuture<Unit> {
        return try {
            PersistenceManager.saveGlobalFlags(changes, clearAll)
        } catch (throwable: Throwable) {
            failedFuture(throwable)
        }
    }

    private fun getFlagAsync(flagName: String): CompletableFuture<IFlag?> {
        return enqueueMutation {
            evictExpiredEntriesInLane().thenCompose { loadFlagInLane(flagName) }
        }
    }

    private fun loadFlagInLane(flagName: String): CompletableFuture<IFlag?> {
        globalFlagMap[flagName]?.let { cached ->
            if (!cached.isTimeout()) return CompletableFuture.completedFuture(cached)
            globalFlagMap.remove(flagName, cached)
            return if (cached.isPersistence) {
                persistGlobalFlags(mapOf(flagName to null)).thenApply { null }
            } else {
                CompletableFuture.completedFuture(null)
            }
        }
        return IStorageManager.INSTANCE.getGlobalFlag(flagName).thenCompose { loaded ->
            when {
                loaded == null -> CompletableFuture.completedFuture(null)
                loaded.isTimeout() -> persistGlobalFlags(mapOf(flagName to null)).thenApply { null }
                else -> {
                    globalFlagMap[flagName] = loaded
                    CompletableFuture.completedFuture(loaded)
                }
            }
        }
    }

    private fun evictExpiredEntriesInLane(): CompletableFuture<Unit> {
        if (globalFlagMap.size <= MAX_CACHE_SIZE) return CompletableFuture.completedFuture(Unit)
        val expired = globalFlagMap.entries.filter { it.value.isTimeout() }
        expired.forEach { (key, flag) -> globalFlagMap.remove(key, flag) }
        val persistent = expired.filter { it.value.isPersistence }.associate { it.key to null }
        return if (persistent.isEmpty()) CompletableFuture.completedFuture(Unit) else persistGlobalFlags(persistent)
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

            return actionFuture { future ->
                run(key).str { it }.thenCompose { flagKey ->
                    run(value).thenCompose { flagValue ->
                        run(persistence).bool { it }.thenCompose { persistent ->
                            run(timeout).long { it }.thenCompose { ticks ->
                                val created = flagValue?.flag(persistent, ticksToMillisSaturated(ticks))
                                    ?: return@thenCompose CompletableFuture.completedFuture(null)
                                setFlagAsync(flagKey, created).thenApply { flagValue }
                            }
                        }
                    }
                }.completeInto(future)
            }
        } catch (_: LocalizedException) {
            reader.reset()
        }

        return actionFuture { future ->
            run(key).str { it }.thenCompose(::getFlagAsync).thenApply { it?.value }.completeInto(future)
        }
    }

    private fun remove(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {

        return actionFuture { future ->
            run(key).str { it }.thenCompose(::removeFlagAsync).completeInto(future)
        }

    }

    private fun clear(reader: QuestReader): ScriptAction<Any?> {

        return actionFuture { future -> clearFlagsAsync().completeInto(future) }
    }

    private fun survival(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {

        return actionFuture { future ->
            run(key).str { it }.thenCompose(::getFlagAsync).thenApply { flag ->
                flag?.let { (System.currentTimeMillis() - it.timestamp).coerceAtLeast(0L) / 50L } ?: 0L
            }.completeInto(future)
        }
    }

    private fun countdown(reader: QuestReader, key: ParsedAction<*>): ScriptAction<Any?> {

        return actionFuture { future ->
            run(key).str { it }.thenCompose(::getFlagAsync).thenApply { flag ->
                flag?.let {
                    if (it.expiresAt == 0L) 0L else positiveDifference(it.expiresAt, System.currentTimeMillis()) / 50L
                } ?: 0L
            }.completeInto(future)
        }
    }

    /**
     * 尝试获取flag
     * @param flagName flag的键名
     * @return flag
     * */
    fun getFlag(flagName: String): IFlag? {
        val flag = globalFlagMap[flagName] ?: return null
        if (!flag.isTimeout()) return flag
        getFlagAsync(flagName).exceptionally { it.printStackTrace(); null }
        return null
    }

    /**
     * 设置flag
     * @param flagName flag的键名
     * @param flag flag
     * @param save 是否检测是否持久并保存
     * */
    fun setFlag(flagName: String, flag: IFlag, save: Boolean = true) {
        setFlagAsync(flagName, flag, save).exceptionally { it.printStackTrace(); null }
    }

    internal fun setFlagAsync(flagName: String, flag: IFlag, save: Boolean = true): CompletableFuture<Boolean> {
        return mutateFlag(flagName, flag, save).thenApply { it != null }
    }

    /**
     * 移除flag
     * @param flagName flag的键名
     * @param save 是否检测是否持久并保存
     * @return 移除的flag
     * */
    fun removeFlag(flagName: String, save: Boolean = true): IFlag? {
        val previous = getFlag(flagName)
        removeFlagAsync(flagName, save).exceptionally { it.printStackTrace(); null }
        return previous
    }

    internal fun removeFlagAsync(flagName: String, save: Boolean = true): CompletableFuture<IFlag?> {
        return mutateFlag(flagName, null, save).thenApply { it?.eventOld }
    }

    private fun mutateFlag(flagName: String, requested: IFlag?, save: Boolean): CompletableFuture<GlobalMutation?> {
        return enqueueMutation {
            loadFlagInLane(flagName).thenCompose { previousOriginal ->
                mainThreadFuture {
                    val event = OrryxGlobalFlagChangeEvents.Pre(flagName, previousOriginal, requested)
                    if (event.call()) event else null
                }.thenCompose { event ->
                    if (event == null) return@thenCompose CompletableFuture.completedFuture(null)
                    val effectiveKey = event.flagName
                    val target = if (effectiveKey == flagName) {
                        CompletableFuture.completedFuture(previousOriginal)
                    } else {
                        loadFlagInLane(effectiveKey)
                    }
                    target.thenComposeMain { previousEffective ->
                        globalFlagMap.remove(flagName)
                        if (effectiveKey != flagName) globalFlagMap.remove(effectiveKey)
                        event.newFlag?.let { globalFlagMap[effectiveKey] = it }
                        val mutation = GlobalMutation(
                            originalKey = flagName,
                            effectiveKey = effectiveKey,
                            eventOld = event.oldFlag,
                            newFlag = event.newFlag,
                            previousOriginal = previousOriginal,
                            previousEffective = previousEffective,
                            save = save,
                        )
                        mutation.persistenceFuture().handle { _, throwable -> throwable }.thenComposeMain { throwable ->
                            if (throwable == null) {
                                OrryxGlobalFlagChangeEvents.Post(
                                    mutation.effectiveKey,
                                    mutation.eventOld,
                                    mutation.newFlag,
                                ).call()
                                CompletableFuture.completedFuture(mutation)
                            } else {
                                rollback(mutation)
                                failedFuture(throwable)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun GlobalMutation.persistenceFuture(): CompletableFuture<Unit> {
        if (!save) return CompletableFuture.completedFuture(Unit)
        val changes = linkedMapOf<String, IFlag?>()
        if (originalKey != effectiveKey && previousOriginal?.isPersistence == true) {
            changes[originalKey] = null
        }
        if (previousEffective?.isPersistence == true || newFlag?.isPersistence == true) {
            changes[effectiveKey] = newFlag?.takeIf { it.isPersistence }
        }
        return if (changes.isEmpty()) CompletableFuture.completedFuture(Unit) else persistGlobalFlags(changes)
    }

    private fun rollback(mutation: GlobalMutation) {
        globalFlagMap.remove(mutation.effectiveKey)
        mutation.previousEffective?.let { globalFlagMap[mutation.effectiveKey] = it }
        if (mutation.originalKey != mutation.effectiveKey) {
            mutation.previousOriginal?.let { globalFlagMap[mutation.originalKey] = it }
        }
    }

    /** 清除所有全局 Flag，并等待持久化删除完成。 */
    fun clearFlags() {
        clearFlagsAsync().exceptionally { it.printStackTrace(); null }
    }

    internal fun clearFlagsAsync(): CompletableFuture<Unit> {
        return enqueueMutation {
            mainThreadFuture {
                val snapshot = globalFlagMap.toMap()
                globalFlagMap.clear()
                snapshot
            }.thenCompose { snapshot ->
                persistGlobalFlags(emptyMap(), clearAll = true).handle { _, throwable ->
                    if (throwable != null) {
                        globalFlagMap.putAll(snapshot)
                        throw java.util.concurrent.CompletionException(throwable)
                    }
                    Unit
                }
            }
        }
    }

    private data class GlobalMutation(
        val originalKey: String,
        val effectiveKey: String,
        val eventOld: IFlag?,
        val newFlag: IFlag?,
        val previousOriginal: IFlag?,
        val previousEffective: IFlag?,
        val save: Boolean,
    )

    private fun positiveDifference(end: Long, start: Long): Long {
        if (end <= start) return 0L
        return if (start < 0L && end > Long.MAX_VALUE + start) Long.MAX_VALUE else end - start
    }

    private fun <T> failedFuture(throwable: Throwable): CompletableFuture<T> {
        return CompletableFuture<T>().also { it.completeExceptionally(throwable) }
    }
}
