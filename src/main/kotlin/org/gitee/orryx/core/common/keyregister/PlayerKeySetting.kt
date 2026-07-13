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
import org.gitee.orryx.dao.persistence.PersistenceManager
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
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
        org.gitee.orryx.utils.mainThreadFuture {
            val onlinePlayer = player
            val event = OrryxPlayerKeySettingSaveEvents.Pre(onlinePlayer, this, async, remove)
            event.call()
            SaveContext(onlinePlayer, event.async, event.remove, createPO())
        }.thenCompose { context ->
            PersistenceManager.saveKey(context.data, context.remove).thenApply { context }
        }.whenComplete { context, throwable ->
            if (throwable != null) {
                throwable.printStackTrace()
            } else {
                org.gitee.orryx.utils.runOnMainThread {
                    callback.run()
                    OrryxPlayerKeySettingSaveEvents.Post(
                        context.player,
                        this@PlayerKeySetting,
                        context.async,
                        context.remove,
                    ).call()
                }
            }
        }
    }

    private data class SaveContext(
        val player: Player,
        val async: Boolean,
        val remove: Boolean,
        val data: PlayerKeySettingPO,
    )

    override fun toString(): String {
        return "PlayerKeySetting(player=$player)"
    }
}
