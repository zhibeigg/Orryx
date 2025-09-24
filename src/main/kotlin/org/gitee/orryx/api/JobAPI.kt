package org.gitee.orryx.api

import org.bukkit.entity.Player
import org.gitee.orryx.api.interfaces.IJobAPI
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.job.JobLoaderManager
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.orryxProfile
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import java.util.concurrent.CompletableFuture
import java.util.function.Function

class JobAPI: IJobAPI {

    override fun <T> modifyJob(player: Player, job: String?, function: Function<IPlayerJob, T>): CompletableFuture<T?> {
        return player.orryxProfile { profile ->
            job?.let {
                player.job(profile.id, it) { job ->
                    function.apply(job)
                }
            } ?: player.job { job ->
                function.apply(job)
            }
        }
    }

    override fun getJob(job: String): IJob? {
        return JobLoaderManager.getJobLoader(job)
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<IJobAPI>(JobAPI())
        }
    }
}