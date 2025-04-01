package org.gitee.orryx.core.profile

import kotlinx.coroutines.launch
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
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
import org.gitee.orryx.utils.toSerializable
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.util.unsafeLazy
import java.util.concurrent.ConcurrentMap

class PlayerProfile(
    override val player: Player,
    private var privateJob: String?,
    private var privatePoint: Int,
    private val privateFlags: ConcurrentMap<String, IFlag>
): IPlayerProfile {

    override val flags: Map<String, IFlag>
        get() = privateFlags

    override val job: String?
        get() = privateJob

    override val point: Int
        get() = privatePoint

    private val superBodyModifier: AttributeModifier by unsafeLazy { AttributeModifier("Orryx@SuperBody", 99999.0, AttributeModifier.Operation.ADD_NUMBER) }

    //霸体过期时间
    private var superBody: Long = 0
    private var change = false

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

    override fun isSuperBody(): Boolean {
        return superBody >= System.currentTimeMillis()
    }

    override fun setSuperBody(timeout: Long) {
        superBody = System.currentTimeMillis() + timeout
        updateSuperBody()
    }

    override fun cancelSuperBody() {
        superBody = 0
        updateSuperBody()
    }

    override fun addSuperBody(timeout: Long) {
        if (isSuperBody()) {
            superBody += timeout
            updateSuperBody()
        } else {
            setSuperBody(timeout)
        }
    }

    override fun reduceSuperBody(timeout: Long) {
        if (isSuperBody()) {
            superBody = (superBody - timeout).coerceAtLeast(0)
            updateSuperBody()
        }
    }

    override fun updateSuperBody() {
        when {
            !isSuperBody() && change -> {
                player.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK)?.removeModifier(superBodyModifier)
                change = false
            }
            isSuperBody() && !change -> {
                player.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK)?.addModifier(superBodyModifier)
                change = true
            }
        }
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
            var var1 = 0
            save {
                var1 ++
                if (var1 == 2) OrryxPlayerJobChangeEvents.Post(player, job).call()
            }
            job.save {
                var1 ++
                if (var1 == 2) OrryxPlayerJobChangeEvents.Post(player, job).call()
            }
        }
    }

    override fun createPO(): PlayerProfilePO {
        return PlayerProfilePO(player.uniqueId, job, point, privateFlags.filter { it.value.isPersistence }.mapValues { it.value.toSerializable() })
    }

    override fun save(async: Boolean, callback: () -> Unit) {
        val data = createPO()
        if (async && !GameManager.shutdown) {
            OrryxAPI.saveScope.launch {
                IStorageManager.INSTANCE.savePlayerData(player.uniqueId, data) {
                    ISyncCacheManager.INSTANCE.removePlayerProfile(player.uniqueId, false)
                    MemoryCache.removePlayerProfile(player.uniqueId)
                    callback()
                }
            }
        } else {
            IStorageManager.INSTANCE.savePlayerData(player.uniqueId, data) {
                ISyncCacheManager.INSTANCE.removePlayerProfile(player.uniqueId, false)
                MemoryCache.removePlayerProfile(player.uniqueId)
                callback()
            }
        }
    }

}