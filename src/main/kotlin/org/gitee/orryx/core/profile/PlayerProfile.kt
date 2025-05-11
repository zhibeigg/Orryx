package org.gitee.orryx.core.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.OrryxPlayerPointEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.dao.cache.ISyncCacheManager
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.async
import org.gitee.orryx.utils.toSerializable
import taboolib.common.platform.function.isPrimaryThread
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentMap

class PlayerProfile(
    override val id: Int,
    val uuid: UUID,
    private var privateJob: String?,
    private var privatePoint: Int,
    private val privateFlags: ConcurrentMap<String, IFlag>
): IPlayerProfile {

    constructor(id: Int, player: Player, privateJob: String?, privatePoint: Int, privateFlags: ConcurrentMap<String, IFlag>): this(id, player.uniqueId, privateJob, privatePoint, privateFlags)

    override val player: Player
        get() = Bukkit.getPlayer(uuid)!!

    override val flags: Map<String, IFlag>
        get() = privateFlags

    override val job: String?
        get() = privateJob

    override val point: Int
        get() = privatePoint

    override fun setFlag(flagName: String, flag: IFlag, save: Boolean) {
        privateFlags[flagName] = flag
        if (save && flag.isPersistence) {
            save(isPrimaryThread)
        }
    }

    override fun getFlag(flagName: String): IFlag? {
        val iterator = privateFlags.iterator()
        while (iterator.hasNext()) {
            val value = iterator.next()
            if (value.value.isTimeout()) {
                iterator.remove()
            }
        }
        return privateFlags[flagName]
    }

    override fun removeFlag(flagName: String, save: Boolean): IFlag? {
        val flag = privateFlags.remove(flagName) ?: return null
        if (save && flag.isPersistence) {
            save(isPrimaryThread)
        }
        return flag
    }

    override fun clearFlags() {
        privateFlags.clear()
        save(isPrimaryThread)
    }

    override fun givePoint(point: Int) {
        if (point < 0) return takePoint(-point)
        val event = OrryxPlayerPointEvents.Up.Pre(player, this, point)
        if (event.call()) {
            privatePoint = (privatePoint + event.point).coerceAtLeast(0)
            save {
                OrryxPlayerPointEvents.Up.Post(player, this, event.point)
            }
        }
    }

    override fun takePoint(point: Int) {
        if (point < 0) return givePoint(-point)
        val event = OrryxPlayerPointEvents.Down.Pre(player, this, point)
        if (event.call()) {
            privatePoint = (privatePoint - event.point).coerceAtLeast(0)
            save {
                OrryxPlayerPointEvents.Down.Post(player, this, event.point)
            }
        }
    }

    override fun setPoint(point: Int) {
        when {
            point > privatePoint -> givePoint(point - privatePoint)
            point < privatePoint -> givePoint(privatePoint - point)
        }
    }

    override fun setJob(job: IPlayerJob) {
        if (OrryxPlayerJobChangeEvents.Pre(player, job).call()) {
            privateJob = job.key
            val future0 = CompletableFuture<Void>()
            val future1 = CompletableFuture<Void>()
            save { future0.complete(null) }
            job.save { future1.complete(null) }
            CompletableFuture.allOf(future0, future1).thenAccept {
                OrryxPlayerJobChangeEvents.Post(player, job).call()
            }
        }
    }

    override fun createPO(): PlayerProfilePO {
        return PlayerProfilePO(id, player.uniqueId, job, point, privateFlags.filter { it.value.isPersistence }.mapValues { it.value.toSerializable() })
    }

    override fun save(async: Boolean, remove: Boolean, callback: () -> Unit) {
        val data = createPO()
        fun remove() {
            if (remove) {
                ISyncCacheManager.INSTANCE.removePlayerProfile(player.uniqueId, false)
                MemoryCache.removePlayerProfile(player.uniqueId)
            }
        }
        if (async && !GameManager.shutdown) {
            OrryxAPI.saveScope.launch(Dispatchers.async) {
                IStorageManager.INSTANCE.savePlayerData(data) {
                    remove()
                    callback()
                }
            }
        } else {
            IStorageManager.INSTANCE.savePlayerData(data) {
                remove()
                callback()
            }
        }
    }
}