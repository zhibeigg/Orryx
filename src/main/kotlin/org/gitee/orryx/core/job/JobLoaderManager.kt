package org.gitee.orryx.core.job

import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.consoleMessage
import org.gitee.orryx.utils.files
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.util.unsafeLazy
import taboolib.module.configuration.Configuration

object JobLoaderManager {

    private val jobLoaderMap by unsafeLazy { hashMapOf<String, IJob>() }

    internal fun getJobLoader(job: String): IJob? {
        return jobLoaderMap[job]
    }

    internal fun getAllJobLoaders(): Map<String, IJob> {
        return jobLoaderMap
    }

    @Reload(weight = 1)
    @Awake(LifeCycle.ENABLE)
    private fun jobReload() {
        jobLoaderMap.clear()
        files("jobs", "example.yml") { file ->
            val configuration = Configuration.loadFromFile(file)
            jobLoaderMap[configuration.name] = JobLoader(configuration.name, configuration)
        }
        consoleMessage("&e┣&7Jobs loaded &e${jobLoaderMap.size} &a√")
    }
}