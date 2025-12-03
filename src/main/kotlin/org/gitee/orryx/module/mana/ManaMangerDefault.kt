package org.gitee.orryx.module.mana

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.core.attribute.Mana
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.OrryxPlayerManaEvents
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.JobLoaderManager
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.dao.cache.ISyncCacheManager
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common5.cdouble
import taboolib.module.kether.orNull
import java.util.concurrent.CompletableFuture

class ManaMangerDefault: IManaManager {

    override fun getMana(player: Player): CompletableFuture<Double> {
        return player.orryxProfileTo {
            it.getFlag(MANA_FLAG)?.value.cdouble
        }
    }

    override fun getMaxMana(player: Player): CompletableFuture<Double> {
        return player.job().thenApply {
            it?.getMaxMana() ?: 0.0
        }
    }

    override fun getMaxMana(player: Player, job: IJob, level: Int): Double {
        val mana = player.eval(job.maxManaActions, mapOf("level" to level)).orNull().cdouble
        var extend = 0.0
        if (NodensPlugin.isEnabled) {
            val valueMap = player.attributeMemory()?.mergedAttribute(Mana.Max)
            valueMap?.forEach { (type, double) ->
                extend += when(type) {
                    PERCENT -> ((valueMap[COUNT]?.get(0) ?: 0.0) + mana) * double[0]
                    COUNT -> double[0]
                }
            }
        }
        return mana
    }

    override fun getMaxMana(player: Player, job: String, level: Int): Double {
        val jobLoader = JobLoaderManager.getJobLoader(job) ?: return 0.0
        return getMaxMana(player, jobLoader, level)
    }

    override fun giveMana(player: Player, mana: Double): CompletableFuture<ManaResult> {
        if (mana < 0) return takeMana(player, -mana)
        val future = CompletableFuture<ManaResult>()
        player.orryxProfile { profile ->
            player.job { job ->
                val event = OrryxPlayerManaEvents.Up.Pre(player, profile, mana)
                if (event.call()) {
                    profile.setFlag(MANA_FLAG, (profile.getFlag(MANA_FLAG)?.value.cdouble + event.mana).coerceIn(0.0, job.getMaxMana()).flag(true), false)
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
                    profile.setFlag(MANA_FLAG, less.coerceIn(0.0, job.getMaxMana()).flag(true), false)
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
        return player.orryxProfileTo { profile ->
            val less = profile.getFlag(MANA_FLAG)?.value.cdouble - mana
            less >= 0
        }
    }

    override fun regainMana(player: Player): CompletableFuture<Double> {
        val future = CompletableFuture<Double>()
        player.orryxProfile { profile ->
            player.job { job ->
                val mana = job.getRegainMana()
                val event = OrryxPlayerManaEvents.Regain.Pre(player, profile, mana)
                if (event.call()) {
                    profile.setFlag(MANA_FLAG, (profile.getFlag(MANA_FLAG)?.value.cdouble + event.regainMana).coerceAtMost(job.getMaxMana()).flag(true), false)
                    save(player, profile) {
                        future.complete(event.regainMana)
                        OrryxPlayerManaEvents.Regain.Post(player, profile, event.regainMana).call()
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
                        OrryxPlayerManaEvents.Heal.Post(player, profile, mana).call()
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