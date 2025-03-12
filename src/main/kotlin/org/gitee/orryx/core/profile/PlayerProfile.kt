package org.gitee.orryx.core.profile

import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI.saveScope
import org.gitee.orryx.api.events.player.OrryxPlayerPointEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.dao.cache.ICacheManager
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.toSerializable
import taboolib.common.platform.function.isPrimaryThread
import java.util.concurrent.ConcurrentMap

class PlayerProfile(override val player: Player, private var privateJob: String?, private var privatePoint: Int, private val privateFlags: ConcurrentMap<String, IFlag>): IPlayerProfile {

    override val flags: Map<String, IFlag>
        get() = privateFlags

    override val job: String?
        get() = privateJob

    override val point: Int
        get() = privatePoint

    //霸体过期时间
    private var superBody: Long = 0

    override fun setFlag(flagName: String, flag: IFlag, save: Boolean) {
        privateFlags[flagName] = flag
        if (flag.isPersistence) {
            save(isPrimaryThread)
        }
    }

    override fun getFlag(flagName: String): IFlag? {
        privateFlags.asSequence().filter { it.value.isTimeout() }.forEach { (key, _) -> privateFlags.remove(key) }
        return privateFlags[flagName]
    }

    override fun removeFlag(flagName: String, save: Boolean): IFlag? {
        val flag = privateFlags.remove(flagName) ?: return null
        if (flag.isPersistence) {
            save(isPrimaryThread)
        }
        return flag
    }

    override fun clearFlags() {
        privateFlags.clear()
        save(isPrimaryThread)
    }

    override fun isSuperBody(): Boolean {
        return superBody >= System.currentTimeMillis()
    }

    override fun setSuperBody(timeout: Long) {
        superBody = System.currentTimeMillis() + timeout
    }

    override fun cancelSuperBody() {
        superBody = 0
    }

    override fun addSuperBody(timeout: Long) {
        if (isSuperBody()) {
            superBody += timeout
        } else {
            setSuperBody(timeout)
        }
    }

    override fun reduceSuperBody(timeout: Long) {
        if (isSuperBody()) {
            superBody = (superBody - timeout).coerceAtLeast(0)
        }
    }

    override fun givePoint(point: Int) {
        if (point < 0) return takePoint(-point)
        val event = OrryxPlayerPointEvents.Up.Pre(player, this, point)
        if (event.call()) {
            privatePoint = (privatePoint + event.point).coerceAtLeast(0)
            save(isPrimaryThread) {
                OrryxPlayerPointEvents.Up.Post(player, this, event.point)
            }
        }
    }

    override fun takePoint(point: Int) {
        if (point < 0) return givePoint(-point)
        val event = OrryxPlayerPointEvents.Down.Pre(player, this, point)
        if (event.call()) {
            privatePoint = (privatePoint - event.point).coerceAtLeast(0)
            save(isPrimaryThread) {
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
            job.save(isPrimaryThread) {
                OrryxPlayerJobChangeEvents.Post(player, job).call()
            }
        }
    }

    private fun createDaoData(): PlayerProfilePO {
        return PlayerProfilePO(player.uniqueId, job, point, privateFlags.filter { it.value.isPersistence }.mapValues { it.value.toSerializable() })
    }

    override fun save(async: Boolean, callback: () -> Unit) {
        val data = createDaoData()
        if (async && !GameManager.shutdown) {
            saveScope.launch {
                IStorageManager.INSTANCE.savePlayerData(player.uniqueId, data)
                ICacheManager.INSTANCE.savePlayerData(player.uniqueId, data, false)
            }.invokeOnCompletion {
                callback()
            }
        } else {
            IStorageManager.INSTANCE.savePlayerData(player.uniqueId, data)
            ICacheManager.INSTANCE.savePlayerData(player.uniqueId, data, false)
            callback()
        }
    }

}