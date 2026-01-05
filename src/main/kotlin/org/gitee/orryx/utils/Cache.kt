package org.gitee.orryx.utils

import com.eatthepath.uuid.FastUUID
import taboolib.common5.cint
import java.util.*

const val PLAYER_DATA = "orryx:player_data:"
const val PLAYER_JOB_DATA = "orryx:player_job_data:"
const val PLAYER_JOB_SKILL_DATA = "orryx:player_job_skill_data:"
const val PLAYER_KEY_SETTING_DATA = "orryx:player_key_setting_data:"

fun playerDataTag(player: UUID): String {
    return "${PLAYER_DATA}${FastUUID.toString(player)}"
}

fun playerJobDataTag(player: UUID, id: Int, job: String): String {
    return "${PLAYER_JOB_DATA}${job}:${FastUUID.toString(player)}:${id}"
}

fun playerJobSkillDataTag(player: UUID, id: Int, job: String, skill: String): String {
    return "${PLAYER_JOB_SKILL_DATA}${job}:${skill}:${FastUUID.toString(player)}:${id}"
}

fun playerKeySettingDataTag(player: UUID): String {
    return "${PLAYER_KEY_SETTING_DATA}${FastUUID.toString(player)}"
}

fun reversePlayerDataTag(flag: String): UUID {
    return flag.removePrefix(PLAYER_DATA).parseUUID()!!
}

fun reversePlayerJobDataTag(flag: String): Triple<String, UUID, Int> {
    return flag.removePrefix(PLAYER_JOB_DATA).split(":").let {
        Triple(it[0], it[1].parseUUID()!!, it[2].cint)
    }
}

class JobSkillDataTag(val id: Int, val player: UUID, val job: String, val skill: String)

fun reversePlayerJobSkillDataTag(flag: String): JobSkillDataTag {
    return flag.removePrefix(PLAYER_JOB_SKILL_DATA).split(":").let {
        JobSkillDataTag(it[3].cint, it[2].parseUUID()!!, it[0], it[1])
    }
}

fun reversePlayerKeySettingTag(flag: String): UUID {
    return flag.removePrefix(PLAYER_KEY_SETTING_DATA).parseUUID()!!
}