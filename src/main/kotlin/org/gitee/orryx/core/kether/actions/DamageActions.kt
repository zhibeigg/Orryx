package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.KetherParser

object DamageActions {

    @KetherParser(["damage"], namespace = NAMESPACE, shared = true)
    private fun damageAction() = combinationParser(
        Action.new("属性系统", "攻击目标", "damage", true)
            .description("攻击目标，支持接入属性系统")
            .addEntry("攻击数值", Type.DOUBLE)
            .addEntry("攻击是否接入属性系统", Type.BOOLEAN, true, default = "false")
            .addContainerEntry("攻击目标")
            .addEntry("攻击来源", Type.CONTAINER, true, "@self", head = "source")
    ) {
        it.group(
            double(),
            bool().option().defaultsTo(false),
            theyContainer(),
            command("source", then = container()).option()
        ).apply(it) { damage, attribute, container, source ->
            now {
                val sources = source.orElse(self())
                if (attribute) {
                    container!!.forEachInstance<ITargetEntity<*>> { target ->
                        target.entity.getBukkitLivingEntity()?.let { entity ->
                            doDamage(sources.firstInstance<PlayerTarget>().player, entity, damage)
                        }
                    }
                } else {
                    container!!.forEachInstance<ITargetEntity<*>> { target ->
                        target.entity.getBukkitLivingEntity()?.let { entity ->
                            doDamage(sources.firstInstance<PlayerTarget>().player, entity, damage)
                        }
                    }
                }
            }
        }
    }


}