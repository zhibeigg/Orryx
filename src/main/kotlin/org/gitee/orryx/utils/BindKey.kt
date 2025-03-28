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
    val map = mutableMapOf<IGroup, MutableMap<IBindKey, String?>>()
    bindKeyOfGroup.forEach {
        val group = BindKeyLoaderManager.getGroup(it.key) ?: return@forEach
        it.value.forEach sec@{ sec ->
            val bindKey = BindKeyLoaderManager.getBindKey(sec.key) ?: return@sec
            map.getOrPut(group) { mutableMapOf() }[bindKey] = sec.value
        }
    }
    return map
}

fun bindKeyOfGroupToMap(bindKeyOfGroup: Map<IGroup, Map<IBindKey, String?>>): Map<String, Map<String, String?>> {
    val map = mutableMapOf<String, MutableMap<String, String?>>()
    bindKeyOfGroup.forEach {
        it.value.forEach sec@{ sec ->
            map.getOrPut(it.key.key) { mutableMapOf() }[sec.key.key] = sec.value
        }
    }
    return map
}