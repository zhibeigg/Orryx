package org.gitee.orryx.api

import com.germ.germplugin.api.GermPacketAPI
import com.germ.germplugin.api.KeyType
import org.bukkit.entity.Player
import org.gitee.orryx.api.interfaces.IKeyAPI
import org.gitee.orryx.compat.dragoncore.DragonCoreCustomPacketSender
import org.gitee.orryx.core.common.keyregister.IKeyRegister
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.core.common.keyregister.PlayerKeySetting
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.utils.*
import priv.seventeen.artist.arcartx.api.ArcartXAPI
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.warning
import java.util.concurrent.CompletableFuture
import java.util.function.Function

class KeyAPI: IKeyAPI {

    override fun bindSkillKeyOfGroup(skill: IPlayerSkill, group: IGroup, bindKey: IBindKey): CompletableFuture<Boolean> {
        return bindSkillKeyOfGroup(skill.player, skill.job, skill.key, group.key, bindKey.key)
    }

    override fun bindSkillKeyOfGroup(player: Player, job: String, skill: String, group: String, bindKey: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        player.getSkill(job, skill).thenAccept { sk ->
            sk ?: error("玩家${player.name}在职业${job}无技能$skill")
            player.job(sk.id, job) { jb ->
                jb.setBindKey(sk, getGroup(group), getBindKey(bindKey)).thenAccept {
                    future.complete(it)
                }
            }
        }
        return future
    }

    override fun <T> modifyKeySetting(player: Player, function: Function<PlayerKeySetting, T>): CompletableFuture<T> {
        return player.keySetting { keySetting ->
            function.apply(keySetting)
        }
    }

    override fun getGroup(key: String): IGroup = BindKeyLoaderManager.getGroup(key) ?: error("未找到组 $key 请在 config.yml 中配置")

    override fun getBindKey(key: String): IBindKey = BindKeyLoaderManager.getBindKey(key) ?: error("未找到绑定按键 $key 请在 keys.yml 中配置")

    override fun updateKeyRegister(player: Player): CompletableFuture<Unit> {
        return player.keySetting { keySetting ->
            when {
                GermPluginPlugin.isEnabled -> {
                    keySetting.keySettingSet().forEach {
                        val key = when (it) {
                            MOUSE_LEFT -> "MLEFT"
                            MOUSE_RIGHT -> "MRIGHT"
                            else -> it
                        }
                        try {
                            GermPacketAPI.sendKeyRegister(player, KeyType.valueOf("KEY_${key}").keyId)
                        } catch (ex: Throwable) {
                            warning("GermPlugin 按键注册失败: ${ex.message}")
                        }
                    }
                }
                DragonCorePlugin.isEnabled -> {
                    try {
                        DragonCoreCustomPacketSender.sendKeyRegister(player, keySetting.keySettingSet())
                    } catch (ex: Throwable) {
                        warning("DragonCore按键注册失败: ${ex.message}")
                    }
                }
                ArcartXPlugin.isEnabled -> {
                    try {
                        keySetting.keySettingSet().forEach {
                            ArcartXAPI.getKeyBindRegistry().registerSimpleKeyBind(it, mutableListOf(it))
                        }
                    } catch (ex: Throwable) {
                        warning("ArcartX按键注册失败: ${ex.message}")
                    }
                }
            }
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