package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.IKeyRegister
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.core.common.keyregister.PlayerKeySetting
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.module.ui.IUIManager
import taboolib.common.platform.function.info
import java.util.concurrent.CompletableFuture

const val MOUSE_LEFT = "MOUSE_LEFT"
const val MOUSE_RIGHT = "MOUSE_RIGHT"
const val LEFT_MENU = "LMENU"

fun getTimeout(): Long {
    return IUIManager.INSTANCE.config.getLong("keyTime", 0)
}

fun getActionType(): IKeyRegister.ActionType {
    return IKeyRegister.ActionType.valueOf(IUIManager.INSTANCE.config.getString("ActionType", "press")!!.uppercase())
}

fun getKeySort(): Boolean {
    return IUIManager.INSTANCE.config.getBoolean("KeySort", false)
}

fun Player.keyPress(key: String, cast: Boolean) {
    val up = key.uppercase()
    val register = KeyRegisterManager.getKeyRegister(uniqueId) ?: return
    if (register.isKeyRelease(up)) {
        register.keyPress(up)
        if (cast) checkAndCast(up, getTimeout(), getActionType(), getKeySort())
    }
}

fun Player.keyRelease(key: String, cast: Boolean) {
    val up = key.uppercase()
    KeyRegisterManager.getKeyRegister(uniqueId)?.keyRelease(up)
    if (cast) checkAndCast(up, getTimeout(), getActionType(), getKeySort())
}

fun Player.checkAndCast(key: String, timeout: Long, actionType: IKeyRegister. ActionType, sort: Boolean) {
    keySetting {
        it.bindKeyMap.forEach { (keyBind, mapping) ->
            if (mapping.split("+").contains(key)) {
                keyBind.checkAndCast(this, mapping.split("+"), timeout, actionType, sort)
            }
        }
    }
}

fun <T> Player.keySetting(func: (setting: PlayerKeySetting) -> T): CompletableFuture<T> {
    return MemoryCache.getPlayerKey(uniqueId).thenApply {
        func(it)
    }
}

fun PlayerKeySetting.keySettingSet(): Set<String> {
    return (bindKeyMap.values.flatMap { it.split("+") } + aimConfirmKey + aimCancelKey + generalAttackKey + blockKey + dodgeKey + extKeyMap.values).map { it.uppercase() }.toSet()
}