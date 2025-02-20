package org.gitee.orryx.utils

import org.gitee.orryx.api.events.player.skill.OrryxClearAllSkillLevelAndBackPointEvent
import org.gitee.orryx.core.job.IPlayerJob

fun IPlayerJob.clearAllLevelAndBackPoint(): Boolean {
    val event = OrryxClearAllSkillLevelAndBackPointEvent(player, this)
    return if (event.call()) {
        job.skills.forEach {
            player.getSkill(job.key, it)?.clearLevelAndBackPoint()
        }
        true
    } else {
        false
    }
}