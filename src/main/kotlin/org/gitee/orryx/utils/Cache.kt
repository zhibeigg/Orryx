package org.gitee.orryx.utils

import com.google.gson.Gson
import java.util.*

val gson by lazy { Gson() }

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
    return java.util.UUID.fromString(flag.removePrefix(PLAYER_DATA))
}

fun reversePlayerJobDataTag(flag: String): List<String> {
    return flag.removePrefix(PLAYER_JOB_DATA).split("_")
}

fun reversePlayerJobSkillDataTag(flag: String): List<String> {
    return flag.removePrefix(PLAYER_JOB_SKILL_DATA).split("_")
}