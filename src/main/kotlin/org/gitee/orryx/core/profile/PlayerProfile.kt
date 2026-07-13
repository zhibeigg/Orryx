package org.gitee.orryx.core.profile

import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.OrryxPlayerFlagChangeEvents
import org.gitee.orryx.api.events.player.OrryxPlayerPointEvents
import org.gitee.orryx.api.events.player.OrryxPlayerProfileSaveEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.dao.cache.ISyncCacheManager
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.persistence.PersistenceManager
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.utils.mainThreadFuture
import org.gitee.orryx.utils.runOnMainThread
import org.gitee.orryx.utils.toSerializable
import taboolib.common.platform.function.isPrimaryThread
import java.util.*
import java.util.concurrent.ConcurrentMap

class PlayerProfile(
    override val id: Int,
    override val uuid: UUID,
    private var privateJob: String?,
    private var privatePoint: Int,
    private val privateFlags: ConcurrentMap<String, IFlag>
): IPlayerProfile {

    constructor(id: Int, player: Player, privateJob: String?, privatePoint: Int, privateFlags: ConcurrentMap<String, IFlag>): this(id, player.uniqueId, privateJob, privatePoint, privateFlags)

    override val player: Player
        get() = Bukkit.getPlayer(uuid) ?: error("Player Offline")

    override val flags: Map<String, IFlag>
        get() = privateFlags

    override val job: String?
        get() = privateJob

    override val point: Int
        get() = privatePoint

    override fun setFlag(flagName: String, flag: IFlag, save: Boolean) {
        val event = OrryxPlayerFlagChangeEvents.Pre(player, this, flagName, privateFlags[flagName], flag)
        if (event.call()) {
            event.oldFlag?.cancel(player, event.flagName)
            val newFlag = event.newFlag
            if (newFlag == null) {
                privateFlags.remove(event.flagName)
            } else {
                privateFlags[event.flagName] = newFlag
                newFlag.init(player, event.flagName)
            }
            if (save && (event.oldFlag?.isPersistence == true || event.newFlag?.isPersistence == true)) {
                save(isPrimaryThread) {
                    OrryxPlayerFlagChangeEvents.Post(player, this, event.flagName, event.oldFlag, event.newFlag).call()
                }
            } else {
                OrryxPlayerFlagChangeEvents.Post(player, this, event.flagName, event.oldFlag, event.newFlag).call()
            }
        }
    }

    override fun getFlag(flagName: String): IFlag? {
        return privateFlags[flagName]
    }

    /** 系统保留 Flag 的内部写入口，不触发可取消的通用 Flag 事件。 */
    internal fun replaceSystemFlag(player: Player, flagName: String, flag: IFlag?) {
        require(player.uniqueId == uuid) { "玩家与 Profile 不匹配" }
        privateFlags.remove(flagName)?.cancel(player, flagName)
        if (flag != null) {
            privateFlags[flagName] = flag
            flag.init(player, flagName)
        }
    }

    override fun removeFlag(flagName: String, save: Boolean): IFlag? {
        val event = OrryxPlayerFlagChangeEvents.Pre(player, this, flagName, privateFlags[flagName], null)
        if (event.call()) {
            val flag = privateFlags.remove(event.flagName) ?: return null
            flag.cancel(player, event.flagName)
            if (save && (event.oldFlag?.isPersistence == true || event.newFlag?.isPersistence == true)) {
                save(isPrimaryThread) {
                    OrryxPlayerFlagChangeEvents.Post(player, this, event.flagName, event.oldFlag, event.newFlag).call()
                }
            } else {
                OrryxPlayerFlagChangeEvents.Post(player, this, event.flagName, event.oldFlag, event.newFlag).call()
            }
            return flag
        } else {
            return null
        }
    }

    override fun clearFlags() {
        privateFlags.forEach {
            it.value.cancel(player, it.key)
        }
        privateFlags.clear()
        save(isPrimaryThread)
    }

    override fun givePoint(point: Int) {
        if (point < 0) return takePoint(-point)
        val event = OrryxPlayerPointEvents.Up.Pre(player, this, point)
        if (event.call()) {
            privatePoint = (privatePoint + event.point).coerceAtLeast(0)
            save {
                OrryxPlayerPointEvents.Up.Post(player, this, event.point).call()
            }
        }
    }

    override fun takePoint(point: Int) {
        if (point < 0) return givePoint(-point)
        val event = OrryxPlayerPointEvents.Down.Pre(player, this, point)
        if (event.call()) {
            privatePoint = (privatePoint - event.point).coerceAtLeast(0)
            save {
                OrryxPlayerPointEvents.Down.Post(player, this, event.point).call()
            }
        }
    }

    override fun setPoint(point: Int) {
        when {
            point > privatePoint -> givePoint(point - privatePoint)
            point < privatePoint -> takePoint(privatePoint - point)
        }
    }

    override fun setJob(job: IPlayerJob) {
        mainThreadFuture {
            val onlinePlayer = player
            if (!OrryxPlayerJobChangeEvents.Pre(onlinePlayer, job).call()) {
                return@mainThreadFuture null
            }
            privateJob = job.key
            Triple(onlinePlayer, createPO(), job.createPO())
        }.thenCompose { context ->
            if (context == null) {
                java.util.concurrent.CompletableFuture.completedFuture(null)
            } else {
                PersistenceManager.saveProfileAndJob(context.second, context.third, invalidate = true)
                    .thenApply { context }
            }
        }.whenComplete { context, throwable ->
            if (throwable != null) {
                throwable.printStackTrace()
            } else if (context != null) {
                runOnMainThread {
                    OrryxPlayerJobChangeEvents.Post(context.first, job).call()
                }
            }
        }
    }

    override fun createPO(): PlayerProfilePO {
        return PlayerProfilePO(id, uuid, job, point, privateFlags.filter { it.value.isPersistence }.mapValues { it.value.toSerializable() })
    }

    override fun save(async: Boolean, remove: Boolean, callback: Runnable) {
        mainThreadFuture {
            val onlinePlayer = player
            val event = OrryxPlayerProfileSaveEvents.Pre(onlinePlayer, this, async, remove)
            event.call()
            SaveContext(onlinePlayer, event.async, event.remove, createPO())
        }.thenCompose { context ->
            PersistenceManager.saveProfile(context.data, context.remove).thenApply { context }
        }.whenComplete { context, throwable ->
            if (throwable != null) {
                throwable.printStackTrace()
            } else {
                runOnMainThread {
                    callback.run()
                    OrryxPlayerProfileSaveEvents.Post(
                        context.player,
                        this@PlayerProfile,
                        context.async,
                        context.remove,
                    ).call()
                }
            }
        }
    }

    private data class SaveContext(
        val player: Player,
        val async: Boolean,
        val remove: Boolean,
        val data: PlayerProfilePO,
    )
}
