package org.gitee.orryx.module.spirit

import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.OrryxPlayerSpiritEvents
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.JobLoaderManager
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.dao.cache.ISyncCacheManager
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.utils.*
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common5.cdouble
import taboolib.module.kether.orNull
import java.util.concurrent.CompletableFuture

class SpiritManagerDefault: ISpiritManager {

    override fun getMaxSpirit(player: Player): CompletableFuture<Double> {
        return player.job().thenApply {
            it?.getMaxSpirit() ?: 0.0
        }
    }

    override fun getMaxSpirit(sender: ProxyCommandSender, job: IJob, level: Int): Double {
        return sender.eval(job.maxSpiritActions, mapOf("level" to level)).orNull().cdouble
    }

    override fun getMaxSpirit(sender: ProxyCommandSender, job: String, level: Int): Double {
        val jobLoader = JobLoaderManager.getJobLoader(job) ?: return 0.0
        return getMaxSpirit(sender, jobLoader, level)
    }

    override fun getSpirit(player: Player): Double {
        // 在线玩家的 Profile 已在 MemoryCache 中，同步取已完成的 future，未加载时兜底 0.0，不阻塞线程
        val profile = MemoryCache.getPlayerProfile(player.uniqueId).getNow(null) ?: return 0.0
        return profile.getFlag(SPIRIT_FLAG)?.value.cdouble
    }

    override fun giveSpirit(player: Player, spirit: Double): CompletableFuture<SpiritResult> {
        if (spirit < 0) return takeSpirit(player, -spirit)
        val future = CompletableFuture<SpiritResult>()
        player.orryxProfile { profile ->
            player.job { job ->
                val event = OrryxPlayerSpiritEvents.Up.Pre(player, profile, spirit)
                if (event.call()) {
                    profile.setFlag(SPIRIT_FLAG, (profile.getFlag(SPIRIT_FLAG)?.value.cdouble + event.spirit).coerceIn(0.0, job.getMaxSpirit()).flag(true), false)
                    save(player, profile) {
                        future.complete(SpiritResult.SUCCESS)
                        OrryxPlayerSpiritEvents.Up.Post(player, profile, event.spirit).call()
                    }
                } else {
                    future.complete(SpiritResult.CANCELLED)
                }
            }.thenApply {
                it ?: future.complete(SpiritResult.NO_JOB)
            }
        }
        return future
    }

    override fun takeSpirit(player: Player, spirit: Double): CompletableFuture<SpiritResult> {
        if (spirit < 0) return giveSpirit(player, -spirit)
        val future = CompletableFuture<SpiritResult>()
        player.orryxProfile { profile ->
            player.job { job ->
                val event = OrryxPlayerSpiritEvents.Down.Pre(player, profile, spirit)
                if (event.call()) {
                    val less = profile.getFlag(SPIRIT_FLAG)?.value.cdouble - event.spirit
                    profile.setFlag(SPIRIT_FLAG, less.coerceIn(0.0, job.getMaxSpirit()).flag(true), false)
                    save(player, profile) {
                        future.complete(
                            if (less >= 0) {
                                SpiritResult.SUCCESS
                            } else {
                                SpiritResult.NOT_ENOUGH
                            }
                        )
                        OrryxPlayerSpiritEvents.Down.Post(player, profile, event.spirit).call()
                    }
                } else {
                    future.complete(SpiritResult.CANCELLED)
                }
            }.thenApply {
                it ?: future.complete(SpiritResult.NO_JOB)
            }
        }
        return future
    }

    override fun haveSpirit(player: Player, spirit: Double): Boolean {
        return getSpirit(player) - spirit >= 0
    }

    override fun healSpirit(player: Player): CompletableFuture<Double> {
        val future = CompletableFuture<Double>()
        player.orryxProfile { profile ->
            player.job { job ->
                val maxSpirit = job.getMaxSpirit()
                val add = maxSpirit - profile.getFlag(SPIRIT_FLAG)?.value.cdouble
                val event = OrryxPlayerSpiritEvents.Heal.Pre(player, profile, add)
                if (event.call()) {
                    profile.setFlag(SPIRIT_FLAG, maxSpirit.flag(true), false)
                    save(player, profile) {
                        future.complete(add)
                        OrryxPlayerSpiritEvents.Heal.Post(player, profile, add).call()
                    }
                } else {
                    future.complete(0.0)
                }
            }.thenApply {
                it ?: future.complete(0.0)
            }
        }
        return future
    }

    override fun regainSpirit(player: Player): CompletableFuture<Double> {
        val future = CompletableFuture<Double>()
        player.orryxProfile { profile ->
            player.job { job ->
                val spirit = job.getRegainSpirit()
                val event = OrryxPlayerSpiritEvents.Regain.Pre(player, profile, spirit)
                if (event.call()) {
                    profile.setFlag(SPIRIT_FLAG, (profile.getFlag(SPIRIT_FLAG)?.value.cdouble + event.regainSpirit).coerceAtMost(job.getMaxSpirit()).flag(true), false)
                    save(player, profile) {
                        future.complete(event.regainSpirit)
                        OrryxPlayerSpiritEvents.Regain.Post(player, profile, event.regainSpirit).call()
                    }
                } else {
                    future.complete(0.0)
                }
            }.thenApply {
                it ?: future.complete(0.0)
            }
        }
        return future
    }

    override fun setSpirit(player: Player, spirit: Double): CompletableFuture<SpiritResult> {
        val currentSpirit = getSpirit(player)
        return when {
            spirit > currentSpirit -> giveSpirit(player, spirit - currentSpirit)
            spirit < currentSpirit -> takeSpirit(player, currentSpirit - spirit)
            else -> CompletableFuture.completedFuture(SpiritResult.SAME)
        }
    }

    private fun save(player: Player, profile: IPlayerProfile, callback: () -> Unit) {
        if (isPrimaryThread && !GameManager.shutdown) {
            OrryxAPI.ioScope.launch {
                ISyncCacheManager.INSTANCE.savePlayerProfile(player.uniqueId, profile.createPO())
                MemoryCache.savePlayerProfile(profile)
            }.invokeOnCompletion {
                callback()
            }
        } else {
            ISyncCacheManager.INSTANCE.savePlayerProfile(player.uniqueId, profile.createPO())
            MemoryCache.savePlayerProfile(profile)
            callback()
        }
    }
}
