package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill
import java.util.concurrent.CompletableFuture

const val DEFAULT = "default"
val MC_KEYS = arrayOf("MC1", "MC2", "MC3", "MC4", "MC5", "MC6", "MC7", "MC8", "MC9")

fun bindKeys(): List<IBindKey> {
    return BindKeyLoaderManager.getBindKeys().values.sortedBy { it.sort }
}

fun IBindKey.getBindSkill(player: Player): CompletableFuture<IPlayerSkill?> {
    val future = CompletableFuture<IPlayerSkill?>()
    player.job {
        it.getBindSkills()[this]?.thenApply { skill ->
            future.complete(skill)
        } ?: future.complete(null)
    }
    return future
}

fun IBindKey.tryCast(player: Player) {
    getBindSkill(player).thenApply { skill ->
        skill?.tryCast()
    }
}

fun bindKeyOfGroupToMutableMap(bindKeyOfGroup: Map<String, Map<String, String?>>): MutableMap<IGroup, MutableMap<IBindKey, String?>> {
    val map = HashMap<IGroup, MutableMap<IBindKey, String?>>()
    bindKeyOfGroup.forEach { entry ->
        val group = BindKeyLoaderManager.getGroup(entry.key) ?: return@forEach
        val bindKeyMap = HashMap<IBindKey, String?>()
        entry.value.forEach sec@{
            val bind = BindKeyLoaderManager.getBindKey(it.key) ?: return@sec
            bindKeyMap[bind] = it.value
        }
        map[group] = bindKeyMap
    }
    return map
}

fun bindKeyOfGroupToMap(bindKeyOfGroup: Map<IGroup, Map<IBindKey, String?>>): Map<String, Map<String, String?>> {
    return bindKeyOfGroup.mapKeys {
        it.key.key
    }.mapValues {
        it.value.mapKeys { entry ->
            entry.key.key
        }
    }
}