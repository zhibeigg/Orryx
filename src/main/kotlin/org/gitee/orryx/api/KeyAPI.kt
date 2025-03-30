package org.gitee.orryx.api

import org.bukkit.entity.Player
import org.gitee.orryx.api.interfaces.IKeyAPI
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.profile.IPlayerKeySetting
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.utils.getSkill
import org.gitee.orryx.utils.job
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.info
import taboolib.module.chat.colored
import java.util.concurrent.CompletableFuture

class KeyAPI: IKeyAPI {

    override val keySetting: IPlayerKeySetting = PlatformFactory.getAPI<IPlayerKeySetting>()

    override fun registerKeySetting(keySetting: IPlayerKeySetting) {
        IPlayerKeySetting.register(keySetting)
        info("&e┣&7外部KeySetting注册成功 &a√".colored())
    }

    override fun bindSkillKeyOfGroup(skill: IPlayerSkill, group: IGroup, bindKey: IBindKey): CompletableFuture<Boolean> {
        return bindSkillKeyOfGroup(skill.player, skill.job, skill.key, group.key, bindKey.key)
    }

    override fun bindSkillKeyOfGroup(player: Player, job: String, skill: String, group: String, bindKey: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        player.getSkill(job, skill).thenApply { sk ->
            player.job(job) { jb ->
                jb.setBindKey(sk ?: error("玩家${player.name}在职业${job}无技能$skill"), getGroup(group), getBindKey(bindKey)).thenApply {
                    future.complete(it)
                }
            }
        }
        return future
    }

    override fun getGroup(key: String): IGroup = BindKeyLoaderManager.getGroup(key) ?: error("未找到组${key}请在config中配置")

    override fun getBindKey(key: String): IBindKey = BindKeyLoaderManager.getBindKey(key) ?: error("未找到绑定按键${key}请在keys中配置")

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<IKeyAPI>(KeyAPI())
        }

    }

}