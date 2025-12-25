package org.gitee.orryx.core.common.keyregister

import com.github.benmanes.caffeine.cache.Caffeine
import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.IKeyRegister.ActionType.PRESS
import org.gitee.orryx.core.common.keyregister.IKeyRegister.ActionType.RELEASE
import java.util.concurrent.TimeUnit

class KeyRegister(override val player: Player): IKeyRegister {

    private val keyCache = Caffeine.newBuilder()
        .initialCapacity(5)
        .maximumSize(100)
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .build<String, KeyAction>()

    private class KeyAction(var lastPress: Long, var lastRelease: Long)

    override fun getKeyPressLast(key: String): Long {
        return keyCache.getIfPresent(key)?.lastPress ?: 0
    }

    override fun getKeyReleaseLast(key: String): Long {
        return keyCache.getIfPresent(key)?.lastRelease ?: 0
    }

    override fun isKeyPress(key: String): Boolean {
        return (keyCache.getIfPresent(key) ?: return false).let {
            it.lastPress > it.lastRelease
        }
    }

    override fun isKeyRelease(key: String): Boolean {
        return (keyCache.getIfPresent(key) ?: return true).let {
            it.lastPress < it.lastRelease
        }
    }

    override fun keyPress(key: String) {
        keyCache.get(key) {
            KeyAction(0, 0)
        }?.lastPress = System.currentTimeMillis()
    }

    override fun keyRelease(key: String) {
        keyCache.get(key) {
            KeyAction(0, 0)
        }?.lastRelease = System.currentTimeMillis()
    }

    override fun isKeyInTimeout(key: String, timeout: Long, actionType: IKeyRegister.ActionType): Boolean {
        return isKeyInTimeout(key, System.currentTimeMillis(), timeout, actionType)
    }

    override fun isKeyInTimeout(key: String, timeStamp: Long, timeout: Long, actionType: IKeyRegister.ActionType): Boolean {
        return (keyCache.getIfPresent(key) ?: return false).let {
            when (actionType) {
                PRESS -> {
                    timeStamp - it.lastPress <= timeout
                }
                RELEASE -> {
                    timeStamp - it.lastRelease <= timeout
                }
            }
        }
    }

    override fun isKeysInTimeout(
        keys: List<String>,
        timeout: Long,
        actionType: IKeyRegister.ActionType,
        sort: Boolean
    ): Boolean {
        return if (sort) {
            keys.forEachIndexed { index, key ->
                val timeStamp = if (index == keys.lastIndex) {
                    System.currentTimeMillis()
                } else {
                    (keyCache.getIfPresent(keys[index + 1]) ?: return false).let {
                        when (actionType) {
                            PRESS -> it.lastPress
                            RELEASE -> it.lastRelease
                        }
                    }
                }
                if (!isKeyInTimeout(key, timeStamp, timeout, actionType)) {
                    return false
                }
            }
            true
        } else {
            keys.all { key ->
                isKeyInTimeout(key, System.currentTimeMillis(), timeout, actionType)
            }
        }
    }
}