package org.gitee.orryx.core.common.keyregister

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
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
import java.util.UUID
import kotlin.Int

class PlayerKeySetting(
    val id: Int,
    val uuid: UUID,
    val bindKeyMap: Map<IBindKey, String>,
    val aimConfirmKey: String = MOUSE_LEFT,
    val aimCancelKey: String = MOUSE_RIGHT,
    val generalAttackKey: String = MOUSE_LEFT,
    val blockKey: String = MOUSE_RIGHT,
    val dodgeKey: String = LEFT_MENU,
    val extKeyMap: Map<String, String> = emptyMap()
): Saveable {

    val player
        get() = Bukkit.getPlayer(uuid) ?: error("Player Offline")

    constructor(player: UUID, playerKeySettingPO: PlayerKeySettingPO) : this(
        playerKeySettingPO.id,
        player,
        bindKeys().associateWith { playerKeySettingPO.bindKeyMap[it.key] ?: it.key },
        playerKeySettingPO.aimConfirmKey,
        playerKeySettingPO.aimCancelKey,
        playerKeySettingPO.generalAttackKey,
        playerKeySettingPO.blockKey,
        playerKeySettingPO.dodgeKey,
        playerKeySettingPO.extKeyMap
    )

    constructor(id: Int, player: Player): this(id, player.uniqueId, bindKeyMap = bindKeys().associateWith { it.key })

    constructor(id: Int, player: UUID): this(id, player, bindKeyMap = bindKeys().associateWith { it.key })

    private fun createPO(): PlayerKeySettingPO {
        return PlayerKeySettingPO(
            id,
            uuid,
            bindKeyMap.mapKeys { it.key.key },
            aimConfirmKey,
            aimCancelKey,
            generalAttackKey,
            blockKey,
            dodgeKey,
            extKeyMap
        )
    }

    override fun save(async: Boolean, remove: Boolean, callback: Runnable) {
        val data = createPO()
        fun remove() {
            if (remove) {
                ISyncCacheManager.INSTANCE.removePlayerKeySetting(player.uniqueId)
                MemoryCache.removePlayerKeySetting(player.uniqueId)
            }
        }
        if (async && !GameManager.shutdown) {
            OrryxAPI.ioScope.launch {
                IStorageManager.INSTANCE.savePlayerKey(data) {
                    remove()
                    callback.run()
                }
            }
        } else {
            IStorageManager.INSTANCE.savePlayerKey(data) {
                remove()
                callback.run()
            }
        }
    }

    override fun toString(): String {
        return "PlayerKeySetting(player=$player)"
    }
}