package org.gitee.orryx.core.mana

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.player.OrryxPlayerManaEvents
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.JobLoaderManager
import org.gitee.orryx.core.profile.PlayerProfileManager.job
import org.gitee.orryx.core.profile.PlayerProfileManager.orryxProfile
import org.gitee.orryx.utils.MANA_FLAG
import org.gitee.orryx.utils.eval
import org.gitee.orryx.utils.flag
import taboolib.common.platform.ProxyCommandSender
import taboolib.common5.cdouble
import taboolib.module.kether.orNull

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

    override fun giveMana(player: Player, mana: Double): ManaResult {
        if (mana < 0) return takeMana(player, -mana)
        val profile = player.orryxProfile()
        val job = player.job() ?: return ManaResult.NO_JOB
        val event = OrryxPlayerManaEvents.Up(player, profile, mana)
        return if (event.call()) {
            profile.setFlag(MANA_FLAG, (profile.getFlag(MANA_FLAG)?.value.cdouble + event.mana).coerceAtLeast(0.0).coerceAtMost(job.getMaxMana()).flag(true))
            profile.save(true)
            ManaResult.SUCCESS
        } else {
            ManaResult.CANCELLED
        }
    }

    override fun takeMana(player: Player, mana: Double): ManaResult {
        if (mana < 0) return giveMana(player, -mana)
        val profile = player.orryxProfile()
        val job = player.job() ?: return ManaResult.NO_JOB
        val event = OrryxPlayerManaEvents.Down(player, profile, mana)
        return if (event.call()) {
            val less = profile.getFlag(MANA_FLAG)?.value.cdouble - event.mana
            profile.setFlag(MANA_FLAG, less.coerceAtLeast(0.0).coerceAtMost(job.getMaxMana()).flag(true))
            profile.save(true)
            if (less >= 0) {
                ManaResult.SUCCESS
            } else {
                ManaResult.NOT_ENOUGH
            }
        } else {
            ManaResult.CANCELLED
        }
    }

    override fun setMana(player: Player, mana: Double): ManaResult {
        val playerMana = getMana(player)
        return when {
            mana > playerMana -> giveMana(player, mana - playerMana)
            mana < playerMana -> takeMana(player, playerMana - mana)
            else -> ManaResult.SAME
        }
    }

    override fun haveMana(player: Player, mana: Double): Boolean {
        val profile = player.orryxProfile()
        val less = profile.getFlag(MANA_FLAG)?.value.cdouble - mana
        return less >= 0
    }

    override fun reginMana(player: Player): Double {
        val profile = player.orryxProfile()
        val job = player.job()
        val mana = job?.getReginMana() ?: return 0.0
        profile.setFlag(MANA_FLAG, (profile.getFlag(MANA_FLAG)?.value.cdouble + mana).coerceAtMost(job.getMaxMana()).flag(true))
        profile.save(true)
        return mana
    }

    override fun healMana(player: Player): Double {
        val profile = player.orryxProfile()
        val job = player.job()
        val mana = job?.getMaxMana() ?: return 0.0
        val add = mana - profile.getFlag(MANA_FLAG)?.value.cdouble
        profile.setFlag(MANA_FLAG, mana.flag(true))
        profile.save(true)
        return add
    }

}