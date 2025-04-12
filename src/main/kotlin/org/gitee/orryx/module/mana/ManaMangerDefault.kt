package org.gitee.orryx.module.mana

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.OrryxPlayerManaEvents
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

class ManaMangerDefault: IManaManager {

    override fun getMana(player: Player): CompletableFuture<Double> {
        return player.orryxProfile {
            it.getFlag(MANA_FLAG)?.value.cdouble
        }
    }

    override fun getMaxMana(player: Player): CompletableFuture<Double> {
        return player.job().thenApply {
            it?.getMaxMana() ?: 0.0
        }
    }

    override fun getMaxMana(sender: ProxyCommandSender, job: IJob, level: Int): Double {
        return sender.eval(job.maxManaActions, mapOf("level" to level)).orNull().cdouble
    }

    override fun getMaxMana(sender: ProxyCommandSender, job: String, level: Int): Double {
        val jobLoader = JobLoaderManager.getJobLoader(job) ?: return 0.0
        return getMaxMana(sender, jobLoader, level)
    }

    override fun giveMana(player: Player, mana: Double): CompletableFuture<ManaResult> {
        if (mana < 0) return takeMana(player, -mana)
        val future = CompletableFuture<ManaResult>()
        player.orryxProfile { profile ->
            player.job { job ->
                val event = OrryxPlayerManaEvents.Up.Pre(player, profile, mana)
                if (event.call()) {
                    profile.setFlag(MANA_FLAG, (profile.getFlag(MANA_FLAG)?.value.cdouble + event.mana).coerceAtLeast(0.0).coerceAtMost(job.getMaxMana()).flag(true), false)
                    save(player, profile) {
                        future.complete(ManaResult.SUCCESS)
                        OrryxPlayerManaEvents.Up.Post(player, profile, event.mana)
                    }
                } else {
                    future.complete(ManaResult.CANCELLED)
                }
            }.thenApply {
                it ?: future.complete(ManaResult.NO_JOB)
            }
        }
        return future
    }

    override fun takeMana(player: Player, mana: Double): CompletableFuture<ManaResult> {
        if (mana < 0) return giveMana(player, -mana)
        val future = CompletableFuture<ManaResult>()
        player.orryxProfile { profile ->
            player.job { job ->
                val event = OrryxPlayerManaEvents.Down.Pre(player, profile, mana)
                if (event.call()) {
                    val less = profile.getFlag(MANA_FLAG)?.value.cdouble - event.mana
                    profile.setFlag(MANA_FLAG, less.coerceAtLeast(0.0).coerceAtMost(job.getMaxMana()).flag(true), false)
                    save(player, profile) {
                        future.complete(
                            if (less >= 0) {
                                ManaResult.SUCCESS
                            } else {
                                ManaResult.NOT_ENOUGH
                            }
                        )
                        OrryxPlayerManaEvents.Down.Post(player, profile, event.mana)
                    }
                } else {
                    future.complete(ManaResult.CANCELLED)
                }
            }.thenApply {
                it ?: future.complete(ManaResult.NO_JOB)
            }
        }
        return future
    }

    override fun setMana(player: Player, mana: Double): CompletableFuture<ManaResult> {
        val future = CompletableFuture<ManaResult>()
        getMana(player).thenApply { playerMana ->
            when {
                mana > playerMana -> giveMana(player, mana - playerMana)
                mana < playerMana -> takeMana(player, playerMana - mana)
                else -> CompletableFuture.completedFuture(ManaResult.SAME)
            }.thenApply {
                future.complete(it)
            }
        }
        return future
    }

    override fun haveMana(player: Player, mana: Double): CompletableFuture<Boolean> {
        return player.orryxProfile { profile ->
            val less = profile.getFlag(MANA_FLAG)?.value.cdouble - mana
            less >= 0
        }
    }

    override fun reginMana(player: Player): CompletableFuture<Double> {
        val future = CompletableFuture<Double>()
        player.orryxProfile { profile ->
            player.job { job ->
                val mana = job.getReginMana()
                val event = OrryxPlayerManaEvents.Regin.Pre(player, profile, mana)
                if (event.call()) {
                    profile.setFlag(MANA_FLAG, (profile.getFlag(MANA_FLAG)?.value.cdouble + mana).coerceAtMost(job.getMaxMana()).flag(true), false)
                    save(player, profile) {
                        future.complete(mana)
                        OrryxPlayerManaEvents.Regin.Post(player, profile, event.reginMana).call()
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

    override fun healMana(player: Player): CompletableFuture<Double> {
        val future = CompletableFuture<Double>()
        player.orryxProfile { profile ->
            player.job { job ->
                val mana = job.getMaxMana()
                val add = mana - profile.getFlag(MANA_FLAG)?.value.cdouble
                val event = OrryxPlayerManaEvents.Heal.Pre(player, profile, add)
                if (event.call()) {
                    profile.setFlag(MANA_FLAG, mana.flag(true), false)
                    save(player, profile) {
                        future.complete(add)
                        OrryxPlayerManaEvents.Heal.Post(player, profile, event.healMana).call()
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

    private fun save(player: Player, profile: IPlayerProfile, callback: () -> Unit) {
        if (isPrimaryThread && !GameManager.shutdown) {
            OrryxAPI.saveScope.launch(Dispatchers.async) {
                ISyncCacheManager.INSTANCE.savePlayerProfile(player.uniqueId, profile.createPO(), false)
                MemoryCache.savePlayerProfile(profile)
            }.invokeOnCompletion {
                callback()
            }
        } else {
            ISyncCacheManager.INSTANCE.savePlayerProfile(player.uniqueId, profile.createPO(), false)
            MemoryCache.savePlayerProfile(profile)
            callback()
        }
    }

}