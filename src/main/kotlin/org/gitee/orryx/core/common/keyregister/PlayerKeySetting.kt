package org.gitee.orryx.core.common.keyregister

import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.key.OrryxPlayerKeySettingSaveEvents
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.dao.cache.ISyncCacheManager
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.cache.Saveable
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.LEFT_MENU
import org.gitee.orryx.utils.MOUSE_LEFT
import org.gitee.orryx.utils.MOUSE_RIGHT
import org.gitee.orryx.utils.bindKeys
import java.util.*

class PlayerKeySetting(
    var id: Int,
    var uuid: UUID,
    val bindKeyMap: MutableMap<IBindKey, String>,
    var aimConfirmKey: String = MOUSE_LEFT,
    var aimCancelKey: String = MOUSE_RIGHT,
    var generalAttackKey: String = MOUSE_LEFT,
    var blockKey: String = MOUSE_RIGHT,
    var dodgeKey: String = LEFT_MENU,
    val extKeyMap: MutableMap<String, String> = mutableMapOf()
): Saveable {

    val player
        get() = Bukkit.getPlayer(uuid) ?: error("Player Offline")

    constructor(player: UUID, playerKeySettingPO: PlayerKeySettingPO) : this(
        playerKeySettingPO.id,
        player,
        bindKeys().associateWith { playerKeySettingPO.bindKeyMap[it.key] ?: it.key }.toMutableMap(),
        playerKeySettingPO.aimConfirmKey,
        playerKeySettingPO.aimCancelKey,
        playerKeySettingPO.generalAttackKey,
        playerKeySettingPO.blockKey,
        playerKeySettingPO.dodgeKey,
        playerKeySettingPO.extKeyMap.toMutableMap()
    )

    constructor(id: Int, player: Player): this(id, player.uniqueId, bindKeyMap = bindKeys().associateWith { it.key }.toMutableMap())

    constructor(id: Int, player: UUID): this(id, player, bindKeyMap = bindKeys().associateWith { it.key }.toMutableMap())

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
        val event = OrryxPlayerKeySettingSaveEvents.Pre(player, this, async, remove)
        event.call()
        val data = createPO()
        fun remove() {
            if (event.remove) {
                ISyncCacheManager.INSTANCE.removePlayerKeySetting(player.uniqueId)
                MemoryCache.removePlayerKeySetting(player.uniqueId)
            }
        }
        if (event.async && !GameManager.shutdown) {
            OrryxAPI.ioScope.launch {
                IStorageManager.INSTANCE.savePlayerKey(data) {
                    remove()
                    callback.run()
                    OrryxPlayerKeySettingSaveEvents.Post(player, this@PlayerKeySetting, event.async, event.remove).call()
                }
            }
        } else {
            IStorageManager.INSTANCE.savePlayerKey(data) {
                remove()
                callback.run()
                OrryxPlayerKeySettingSaveEvents.Post(player, this, event.async, event.remove).call()
            }
        }
    }

    override fun toString(): String {
        return "PlayerKeySetting(player=$player)"
    }
}
