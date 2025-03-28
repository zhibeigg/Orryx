package org.gitee.orryx.utils

import taboolib.common5.util.parseUUID
import java.util.*

const val PLAYER_DATA = "orryx_player_data_"
const val PLAYER_JOB_DATA = "orryx_player_job_data_"
const val PLAYER_JOB_SKILL_DATA = "orryx_player_job_skill_data_"

fun playerDataTag(player: UUID): String {
    return "${PLAYER_DATA}${player}"
}

fun playerJobDataTag(player: UUID, job: String): String {
    return "${PLAYER_JOB_DATA}${job}_${player}"
}

fun playerJobSkillDataTag(player: UUID, job: String, skill: String): String {
    return "${PLAYER_JOB_SKILL_DATA}${job}_${skill}_${player}"
}

fun reversePlayerDataTag(flag: String): UUID {
    return flag.removePrefix(PLAYER_DATA).parseUUID()!!
}

fun reversePlayerJobDataTag(flag: String): Pair<String, UUID> {
    return flag.removePrefix(PLAYER_JOB_DATA).split("_").let {
        it[it.lastIndex-1] to it.last().parseUUID()!!
    }
}

class JobSkillDataTag(val player: UUID, val job: String, val skill: String)

fun reversePlayerJobSkillDataTag(flag: String): JobSkillDataTag {
    return flag.removePrefix(PLAYER_JOB_SKILL_DATA).split("_").let {
        JobSkillDataTag(it.last().parseUUID()!!, it[it.lastIndex-2], it[it.lastIndex-1])
    }
}