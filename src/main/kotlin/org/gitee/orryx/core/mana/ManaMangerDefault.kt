package org.gitee.orryx.core.mana

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.player.OrryxPlayerManaEvents
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.JobLoaderManager
import org.gitee.orryx.core.profile.PlayerProfileManager.orryxProfile
import org.gitee.orryx.utils.MANA_FLAG
import org.gitee.orryx.utils.eval
import org.gitee.orryx.utils.flag
import org.gitee.orryx.utils.job
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common5.cdouble
import taboolib.module.kether.orNull
import java.util.concurrent.CompletableFuture

class ManaMangerDefault: IManaManager {

    override fun getMana(player: Player): Double {
        return player.orryxProfile().getFlag(MANA_FLAG)?.value.cdouble
    }

    override fun getMaxMana(player: Player): Double {
        val job = player.job() ?: return 0.0
        return job.getMaxMana()
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
        val profile = player.orryxProfile()
        val job = player.job() ?: return CompletableFuture.completedFuture(ManaResult.NO_JOB)
        val future = CompletableFuture<ManaResult>()
        val event = OrryxPlayerManaEvents.Up.Pre(player, profile, mana)
        if (event.call()) {
            profile.setFlag(MANA_FLAG, (profile.getFlag(MANA_FLAG)?.value.cdouble + event.mana).coerceAtLeast(0.0).coerceAtMost(job.getMaxMana()).flag(true), false)
            profile.save(isPrimaryThread) {
                future.complete(ManaResult.SUCCESS)
                OrryxPlayerManaEvents.Up.Post(player, profile, event.mana)
            }
        } else {
            future.complete(ManaResult.CANCELLED)
        }
        return future
    }

    override fun takeMana(player: Player, mana: Double): CompletableFuture<ManaResult> {
        if (mana < 0) return giveMana(player, -mana)
        val profile = player.orryxProfile()
        val job = player.job() ?: return CompletableFuture.completedFuture(ManaResult.NO_JOB)
        val future = CompletableFuture<ManaResult>()
        val event = OrryxPlayerManaEvents.Down.Pre(player, profile, mana)
        if (event.call()) {
            val less = profile.getFlag(MANA_FLAG)?.value.cdouble - event.mana
            profile.setFlag(MANA_FLAG, less.coerceAtLeast(0.0).coerceAtMost(job.getMaxMana()).flag(true), false)
            profile.save(isPrimaryThread) {
                future.complete(if (less >= 0) {
                    ManaResult.SUCCESS
                } else {
                    ManaResult.NOT_ENOUGH
                })
                OrryxPlayerManaEvents.Down.Post(player, profile, event.mana)
            }
        } else {
            future.complete(ManaResult.CANCELLED)
        }
        return future
    }

    override fun setMana(player: Player, mana: Double): CompletableFuture<ManaResult> {
        val playerMana = getMana(player)
        return when {
            mana > playerMana -> giveMana(player, mana - playerMana)
            mana < playerMana -> takeMana(player, playerMana - mana)
            else -> CompletableFuture.completedFuture(ManaResult.SAME)
        }
    }

    override fun haveMana(player: Player, mana: Double): Boolean {
        val profile = player.orryxProfile()
        val less = profile.getFlag(MANA_FLAG)?.value.cdouble - mana
        return less >= 0
    }

    override fun reginMana(player: Player): CompletableFuture<Double> {
        val profile = player.orryxProfile()
        val job = player.job()
        val mana = job?.getReginMana() ?: return CompletableFuture.completedFuture(0.0)
        val future = CompletableFuture<Double>()
        val event = OrryxPlayerManaEvents.Regin.Pre(player, profile, mana)
        if (event.call()) {
            profile.setFlag(MANA_FLAG, (profile.getFlag(MANA_FLAG)?.value.cdouble + mana).coerceAtMost(job.getMaxMana()).flag(true), false)
            profile.save(isPrimaryThread) {
                future.complete(mana)
                OrryxPlayerManaEvents.Regin.Post(player, profile, event.reginMana).call()
            }
        } else {
            future.complete(0.0)
        }
        return future
    }

    override fun healMana(player: Player): CompletableFuture<Double> {
        val profile = player.orryxProfile()
        val job = player.job()
        val mana = job?.getMaxMana() ?: return CompletableFuture.completedFuture(0.0)
        val future = CompletableFuture<Double>()
        val add = mana - profile.getFlag(MANA_FLAG)?.value.cdouble
        val event = OrryxPlayerManaEvents.Heal.Pre(player, profile, add)
        if (event.call()) {
            profile.setFlag(MANA_FLAG, mana.flag(true), false)
            profile.save(isPrimaryThread) {
                future.complete(add)
                OrryxPlayerManaEvents.Heal.Post(player, profile, event.healMana).call()
            }
        } else {
            future.complete(0.0)
        }
        return future
    }

}