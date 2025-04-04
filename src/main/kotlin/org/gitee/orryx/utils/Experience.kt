package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.module.experience.IExperience
import taboolib.common.platform.function.adaptCommandSender

fun IExperience.getExperienceOfLevel(player: Player, level: Int): Int {
    return getExperienceOfLevel(adaptCommandSender(player), level)
}

fun IExperience.getLevel(player: Player, experience: Int): Int {
    return getLevelAndLessExp(player, experience).first
}

fun IExperience.getLessExp(player: Player, experience: Int): Int {
    return getLevelAndLessExp(player, experience).second
}

fun IExperience.getLevelAndLessExp(player: Player, experience: Int): Pair<Int, Int> {
    var exp = experience.coerceAtLeast(0)
    var level = minLevel
    while (exp > 0) {
        val max = getExperienceOfLevel(player, level)
        if (max > exp) break
        if (level >= maxLevel) {
            level = maxLevel
            exp = 0
            break
        }
        level ++
        exp -= max
    }
    return level to exp
}

fun IExperience.getExperienceUntilLevel(player: Player, level: Int): Int {
    var experience = 0
    for (i in minLevel until level.coerceAtLeast(minLevel).coerceAtMost(maxLevel)) {
        experience += getExperienceOfLevel(player, i)
    }
    return experience
}

fun IExperience.maxExp(player: Player): Int {
    return getExperienceUntilLevel(player, maxLevel)
}

/**
 * 获取经验从from升级到to
 * */
fun IExperience.getExperienceFromTo(player: Player, from: Int, to: Int): Int {
    var experience = 0
    for(i in from until to) {
        experience += getExperienceOfLevel(player, i)
    }
    return experience
}