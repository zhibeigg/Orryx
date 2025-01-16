package org.gitee.orryx.core.profile

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.player.OrryxPlayerJobChangeEvent
import org.gitee.orryx.api.events.player.OrryxPlayerPointEvents
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.dao.cache.ICacheManager
import org.gitee.orryx.dao.pojo.PlayerData
import org.gitee.orryx.dao.storage.IStorageManager
import taboolib.common.platform.function.submitAsync

class PlayerProfile(override val player: Player, private var privateJob: String?, private var privatePoint: Int, private val privateFlags: MutableMap<String, IFlag<*>>): IPlayerProfile {

    override val flags: Map<String, IFlag<*>>
        get() = privateFlags

    override val job: String?
        get() = privateJob

    override val point: Int
        get() = privatePoint

    //霸体过期时间
    private var superBody: Long = 0

    override fun setFlag(flagName: String, flag: IFlag<*>) {
        privateFlags[flagName] = flag
        if (flag.isPersistence) {
            save(true)
        }
    }

    override fun getFlag(flagName: String): IFlag<*>? {
        privateFlags.asSequence().filter { it.value.isTimeout() }.forEach { (key, _) -> privateFlags.remove(key) }
        save(true)
        return privateFlags[flagName]
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
        val event = OrryxPlayerPointEvents.Down(player, this, point)
        if (event.call()) {
            privatePoint = (privatePoint + event.point).coerceAtLeast(0)
            save(true)
        }
    }

    override fun takePoint(point: Int) {
        if (point < 0) return givePoint(-point)
        val event = OrryxPlayerPointEvents.Down(player, this, point)
        if (event.call()) {
            privatePoint = (privatePoint - event.point).coerceAtLeast(0)
            save(true)
        }
    }

    override fun setPoint(point: Int) {
        when {
            point > privatePoint -> givePoint(point - privatePoint)
            point < privatePoint -> givePoint(privatePoint - point)
        }
    }

    override fun setJob(job: IPlayerJob) {
        if (OrryxPlayerJobChangeEvent(player, job).call()) {
            privateJob = job.key
            job.save(true)
        }
    }

    private fun createDaoData(): PlayerData {
        return PlayerData(player.uniqueId, job, point, privateFlags.filter { it.value.isPersistence })
    }

    override fun save(async: Boolean) {
        val data = createDaoData()
        if (async) {
            submitAsync {
                IStorageManager.INSTANCE.savePlayerData(player.uniqueId, data)
                ICacheManager.INSTANCE.savePlayerData(player.uniqueId, data, false)
            }
        } else {
            IStorageManager.INSTANCE.savePlayerData(player.uniqueId, data)
            ICacheManager.INSTANCE.savePlayerData(player.uniqueId, data, false)
        }
    }

}