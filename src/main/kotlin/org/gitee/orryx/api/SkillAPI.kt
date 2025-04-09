package org.gitee.orryx.api

import org.bukkit.entity.Player
import org.gitee.orryx.api.interfaces.ISkillAPI
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.skill.*
import org.gitee.orryx.utils.castSkill
import org.gitee.orryx.utils.getSkill
import org.gitee.orryx.utils.skill
import org.gitee.orryx.utils.tryCast
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import java.util.concurrent.CompletableFuture

class SkillAPI: ISkillAPI {

    override fun <T> modifySkill(
        player: Player,
        skill: String,
        job: IPlayerJob?,
        function: (skill: IPlayerSkill) -> T
    ): CompletableFuture<T?> {
        return job?.let {
            player.skill(it, skill, false, function)
        } ?: player.skill(skill, false, function)
    }

    override fun castSkill(player: Player, skill: String, level: Int) {
        (SkillLoaderManager.getSkillLoader(skill) as ICastSkill).castSkill(player, SkillParameter(skill, player, level), false)
    }

    override fun tryCastSkill(player: Player, skill: String): CompletableFuture<CastResult?> {
        return player.getSkill(skill).thenCompose {
            it?.tryCast()
        }
    }

    override fun getSkill(skill: String): ISkill? {
        return SkillLoaderManager.getSkillLoader(skill)
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<ISkillAPI>(SkillAPI())
        }

    }

}