package org.gitee.orryx.api

import org.bukkit.entity.Player
import org.gitee.orryx.api.interfaces.IKeyAPI
import org.gitee.orryx.compat.KeyRegisterSenderManager
import org.gitee.orryx.core.common.keyregister.IKeyRegister
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.core.common.keyregister.PlayerKeySetting
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.utils.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import java.util.concurrent.CompletableFuture
import java.util.function.Function

class KeyAPI: IKeyAPI {

    override fun bindSkillKeyOfGroup(skill: IPlayerSkill, group: IGroup, bindKey: IBindKey): CompletableFuture<Boolean> {
        return bindSkillKeyOfGroup(skill.player, skill.job, skill.key, group.key, bindKey.key)
    }

    override fun bindSkillKeyOfGroup(player: Player, job: String, skill: String, group: String, bindKey: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        try {
            player.getSkill(job, skill).thenAccept { sk ->
                if (sk == null) {
                    future.complete(false)
                    return@thenAccept
                }
                player.job(sk.id, job) { jb ->
                    jb.setBindKey(sk, getGroupOrThrow(group), getBindKeyOrThrow(bindKey)).thenAccept {
                        future.complete(it)
                    }.exceptionally { ex ->
                        future.completeExceptionally(ex)
                        null
                    }
                }
            }.exceptionally { ex ->
                future.completeExceptionally(ex)
                null
            }
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
        return future
    }

    override fun <T> modifyKeySetting(player: Player, function: Function<PlayerKeySetting, T>): CompletableFuture<T> {
        return player.keySetting { keySetting ->
            function.apply(keySetting)
        }
    }

    override fun getGroup(key: String): IGroup? = BindKeyLoaderManager.getGroup(key)

    override fun getBindKey(key: String): IBindKey? = BindKeyLoaderManager.getBindKey(key)

    override fun updateKeyRegister(player: Player): CompletableFuture<Unit> {
        return player.keySetting { keySetting ->
            KeyRegisterSenderManager.getSender()?.sendKeyRegister(player, keySetting.keySettingSet())
        }
    }

    override fun getKeyRegister(player: Player): IKeyRegister? {
        return KeyRegisterManager.getKeyRegister(player.uniqueId)
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<IKeyAPI>(KeyAPI())
        }
    }
}
