package org.gitee.orryx.utils

import com.germ.germplugin.api.GermKeyAPI
import com.germ.germplugin.api.KeyType
import eos.moe.dragoncore.api.CoreAPI
import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.IKeyRegister
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.core.common.keyregister.PlayerKeySetting
import org.gitee.orryx.core.key.BindKeyLoader
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.module.ui.IUIManager

const val MOUSE_LEFT = "MOUSE_LEFT"
const val MOUSE_RIGHT = "MOUSE_RIGHT"

fun keysRegister(keys: Collection<BindKeyLoader>) {
    if (DragonCorePlugin.isEnabled) {
        keys.forEach { bindKey ->
            bindKey.keys.forEach { key ->
                CoreAPI.registerKey(key)
            }
        }
    }
    if (GermPluginPlugin.isEnabled) {
        keys.forEach { bindKey ->
            bindKey.keys.forEach { key ->
                GermKeyAPI.registerKey(KeyType.valueOf("KEY_$key"))
            }
        }
    }
}

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
    KeyRegisterManager.getKeyRegister(uniqueId)?.keyPress(up)
    if (cast) checkAndCast(up, getTimeout(), getActionType(), getKeySort())
}

fun Player.keyRelease(key: String, cast: Boolean) {
    val up = key.uppercase()
    KeyRegisterManager.getKeyRegister(uniqueId)?.keyRelease(up)
    if (cast) checkAndCast(up, getTimeout(), getActionType(), getKeySort())
}

fun Player.checkAndCast(key: String, timeout: Long, actionType: IKeyRegister. ActionType, sort: Boolean) {
    bindKeys().forEach {
        if (it.key.split("+").contains(key)) {
            it.checkAndCast(this, timeout, actionType, sort)
        }
    }
}

fun <T> Player.keySetting(func: (setting: PlayerKeySetting) -> T) {
    MemoryCache.getPlayerKey(this).thenApply {
        func(it)
    }
}