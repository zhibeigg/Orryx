package org.gitee.orryx.utils

import com.germ.germplugin.api.GermKeyAPI
import com.germ.germplugin.api.KeyType
import eos.moe.dragoncore.api.CoreAPI
import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.IKeyRegister
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.core.key.BindKeyLoader
import org.gitee.orryx.core.ui.IUIManager

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