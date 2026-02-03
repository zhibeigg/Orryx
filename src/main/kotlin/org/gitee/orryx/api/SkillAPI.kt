package org.gitee.orryx.api

import org.bukkit.entity.Player
import org.gitee.orryx.api.interfaces.ISkillAPI
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.kether.parameter.SkillTrigger
import org.gitee.orryx.core.skill.*
import org.gitee.orryx.utils.castSkill
import org.gitee.orryx.utils.getSkill
import org.gitee.orryx.utils.skill
import org.gitee.orryx.utils.tryCast
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import java.util.concurrent.CompletableFuture
import java.util.function.Function

class SkillAPI: ISkillAPI {

    override fun <T> modifySkill(
        player: Player,
        skill: String,
        job: IPlayerJob?,
        function: Function<IPlayerSkill, T>
    ): CompletableFuture<T?> {
        return job?.let {
            player.skill(it, skill, false) { skill ->
                function.apply(skill)
            }
        } ?: player.skill(skill, false) {
            function.apply(it)
        }
    }

    override fun castSkill(player: Player, skill: String, level: Int) {
        val parameter = SkillParameter(skill, player, level).apply {
            trigger = SkillTrigger.Api("SkillAPI")
        }
        (SkillLoaderManager.getSkillLoader(skill) as ICastSkill).castSkill(player, parameter, false)
    }

    override fun tryCastSkill(player: Player, skill: String): CompletableFuture<CastResult?> {
        return player.getSkill(skill).thenCompose {
            it?.tryCast(SkillTrigger.Api("SkillAPI"))
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