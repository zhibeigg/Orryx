package org.gitee.orryx.utils

import com.eatthepath.uuid.FastUUID
import java.util.*

/**
 * Redis 键规范:
 * - 统一前缀: orryx:player:{uuid}:...
 * - UUID 前置便于按玩家批量扫描
 * - 使用冒号分隔层级
 */
private const val PREFIX = "orryx:player:"
private const val PROFILE_SUFFIX = ":profile"
private const val JOB_INFIX = ":job:"
private const val SKILL_INFIX = ":skill:"
private const val KEYSETTING_SUFFIX = ":keysetting"

// UUID 长度 (FastUUID 格式)
private const val UUID_LEN = 36

/**
 * 玩家基础数据键: orryx:player:{uuid}:profile
 */
fun playerDataTag(player: UUID): String {
    return buildString(PREFIX.length + UUID_LEN + PROFILE_SUFFIX.length) {
        append(PREFIX)
        append(FastUUID.toString(player))
        append(PROFILE_SUFFIX)
    }
}

/**
 * 玩家职业数据键: orryx:player:{uuid}:job:{job}:{id}
 */
fun playerJobDataTag(player: UUID, id: Int, job: String): String {
    return buildString(PREFIX.length + UUID_LEN + JOB_INFIX.length + job.length + 12) {
        append(PREFIX)
        append(FastUUID.toString(player))
        append(JOB_INFIX)
        append(job)
        append(':')
        append(id)
    }
}

/**
 * 玩家技能数据键: orryx:player:{uuid}:skill:{job}:{skill}:{id}
 */
fun playerJobSkillDataTag(player: UUID, id: Int, job: String, skill: String): String {
    return buildString(PREFIX.length + UUID_LEN + SKILL_INFIX.length + job.length + skill.length + 14) {
        append(PREFIX)
        append(FastUUID.toString(player))
        append(SKILL_INFIX)
        append(job)
        append(':')
        append(skill)
        append(':')
        append(id)
    }
}

/**
 * 玩家按键设置键: orryx:player:{uuid}:keysetting
 */
fun playerKeySettingDataTag(player: UUID): String {
    return buildString(PREFIX.length + UUID_LEN + KEYSETTING_SUFFIX.length) {
        append(PREFIX)
        append(FastUUID.toString(player))
        append(KEYSETTING_SUFFIX)
    }
}

/**
 * 反向解析玩家数据键
 */
fun reversePlayerDataTag(tag: String): UUID {
    // orryx:player:{uuid}:profile -> 提取 uuid 部分
    return tag.substring(PREFIX.length, PREFIX.length + UUID_LEN).parseUUID()!!
}

/**
 * 反向解析玩家职业数据键
 */
fun reversePlayerJobDataTag(tag: String): Triple<String, UUID, Int> {
    // orryx:player:{uuid}:job:{job}:{id}
    val uuid = tag.substring(PREFIX.length, PREFIX.length + UUID_LEN).parseUUID()!!
    val rest = tag.substring(PREFIX.length + UUID_LEN + JOB_INFIX.length)
    val colonIdx = rest.lastIndexOf(':')
    val job = rest.substring(0, colonIdx)
    val id = rest.substring(colonIdx + 1).toInt()
    return Triple(job, uuid, id)
}

/**
 * 职业技能数据标签解析结果
 */
class JobSkillDataTag(val id: Int, val player: UUID, val job: String, val skill: String)

/**
 * 反向解析玩家技能数据键
 */
fun reversePlayerJobSkillDataTag(tag: String): JobSkillDataTag {
    // orryx:player:{uuid}:skill:{job}:{skill}:{id}
    val uuid = tag.substring(PREFIX.length, PREFIX.length + UUID_LEN).parseUUID()!!
    val rest = tag.substring(PREFIX.length + UUID_LEN + SKILL_INFIX.length)

    // 从后往前解析: {job}:{skill}:{id}
    val lastColon = rest.lastIndexOf(':')
    val id = rest.substring(lastColon + 1).toInt()

    val beforeId = rest.substring(0, lastColon)
    val secondLastColon = beforeId.lastIndexOf(':')
    val skill = beforeId.substring(secondLastColon + 1)
    val job = beforeId.substring(0, secondLastColon)

    return JobSkillDataTag(id, uuid, job, skill)
}

/**
 * 反向解析玩家按键设置键
 */
fun reversePlayerKeySettingTag(tag: String): UUID {
    // orryx:player:{uuid}:keysetting -> 提取 uuid 部分
    return tag.substring(PREFIX.length, PREFIX.length + UUID_LEN).parseUUID()!!
}