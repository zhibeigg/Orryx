package org.gitee.orryx.core.common.keyregister

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.dao.cache.ISyncCacheManager
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.cache.Saveable
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.*

class PlayerKeySetting(
    val player: Player,
    val bindKeyMap: Map<IBindKey, String>,
    val aimConfirmKey: String = MOUSE_LEFT,
    val aimCancelKey: String = MOUSE_RIGHT,
    val generalAttackKey: String = MOUSE_LEFT,
    val blockKey: String = MOUSE_RIGHT,
    val dodgeKey: String = LEFT_MENU,
    val extKeyMap: Map<String, String> = emptyMap()
): Saveable {

    constructor(player: Player, playerKeySettingPO: PlayerKeySettingPO) : this(
        player,
        bindKeys().associateWith { playerKeySettingPO.bindKeyMap[it.key] ?: it.key },
        playerKeySettingPO.aimConfirmKey,
        playerKeySettingPO.aimCancelKey,
        playerKeySettingPO.generalAttackKey,
        playerKeySettingPO.blockKey,
        playerKeySettingPO.dodgeKey,
        playerKeySettingPO.extKeyMap
    )

    constructor(player: Player): this(player, bindKeyMap = bindKeys().associateWith { it.key })

    private fun createPO(): PlayerKeySettingPO {
        return PlayerKeySettingPO(
            bindKeyMap.mapKeys { it.key.key },
            aimConfirmKey,
            aimCancelKey,
            generalAttackKey,
            blockKey,
            dodgeKey,
            extKeyMap
        )
    }

    override fun save(async: Boolean, remove: Boolean, callback: () -> Unit) {
        val data = createPO()
        fun remove() {
            if (remove) {
                ISyncCacheManager.INSTANCE.removePlayerKeySetting(player.uniqueId, false)
                MemoryCache.removePlayerKeySetting(player.uniqueId)
            }
        }
        if (async && !GameManager.shutdown) {
            OrryxAPI.saveScope.launch(Dispatchers.async) {
                IStorageManager.INSTANCE.savePlayerKey(player.uniqueId, data) {
                    remove()
                    callback()
                }
            }
        } else {
            IStorageManager.INSTANCE.savePlayerKey(player.uniqueId, data) {
                remove()
                callback()
            }
        }
    }

    override fun toString(): String {
        return "PlayerKeySetting(player=$player)"
    }
}