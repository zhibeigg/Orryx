package org.gitee.orryx.module.spirit

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.events.player.OrryxPlayerSpiritEvents
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.JobLoaderManager
import org.gitee.orryx.utils.eval
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.orryxProfile
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common5.cdouble
import taboolib.module.kether.orNull
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class SpiritManagerDefault: ISpiritManager {

    companion object {

        val spiritMap = ConcurrentHashMap<UUID, Double>()

        @SubscribeEvent
        private fun quit(e: PlayerQuitEvent) {
            spiritMap.remove(e.player.uniqueId)
        }
    }

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
        return spiritMap.getOrPut(player.uniqueId) { 0.0 }
    }

    override fun giveSpirit(player: Player, spirit: Double): CompletableFuture<SpiritResult> {
        if (spirit < 0) return takeSpirit(player, -spirit)
        val future = CompletableFuture<SpiritResult>()
        player.orryxProfile { profile ->
            player.job { job ->
                val event = OrryxPlayerSpiritEvents.Up(player, profile, spirit)
                if (event.call()) {
                    spiritMap[player.uniqueId] = (spiritMap.getOrPut(player.uniqueId) { 0.0 } + event.spirit).coerceIn(0.0, job.getMaxSpirit())
                    future.complete(SpiritResult.SUCCESS)
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
                val event = OrryxPlayerSpiritEvents.Down(player, profile, spirit)
                if (event.call()) {
                    val less = spiritMap.getOrPut(player.uniqueId) { 0.0 } - event.spirit
                    spiritMap[player.uniqueId] = less.coerceIn(0.0, job.getMaxSpirit())
                    future.complete(
                        if (less >= 0) {
                            SpiritResult.SUCCESS
                        } else {
                            SpiritResult.NOT_ENOUGH
                        }
                    )
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
        val less = spiritMap.getOrPut(player.uniqueId) { 0.0 } - spirit
        return less >= 0
    }

    override fun healSpirit(player: Player): CompletableFuture<Double> {
        val future = CompletableFuture<Double>()
        player.orryxProfile { profile ->
            player.job { job ->
                val spirit = job.getMaxSpirit()
                val add = spirit - spiritMap.getOrPut(player.uniqueId) { 0.0 }
                val event = OrryxPlayerSpiritEvents.Heal(player, profile, add)
                if (event.call()) {
                    spiritMap[player.uniqueId] = spirit
                    future.complete(add)
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
                val event = OrryxPlayerSpiritEvents.Regain(player, profile, spirit)
                if (event.call()) {
                    spiritMap[player.uniqueId] = (spiritMap.getOrPut(player.uniqueId) { 0.0 } + event.regainSpirit).coerceAtMost(job.getMaxSpirit())
                    future.complete(event.regainSpirit)
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
        val future = CompletableFuture<SpiritResult>()
        getSpirit(player).let { playerSpirit ->
            when {
                spirit > playerSpirit -> giveSpirit(player, spirit - playerSpirit)
                spirit < playerSpirit -> takeSpirit(player, playerSpirit - spirit)
                else -> future.complete(SpiritResult.SAME)
            }
        }
        return future
    }
}