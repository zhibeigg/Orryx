package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.compat.DefaultAttributeBridge
import org.gitee.orryx.compat.IAttributeBridge
import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.KetherParser

object DamageActions {

    internal val Default by lazy { DefaultAttributeBridge() }

    @KetherParser(["damage"], namespace = NAMESPACE, shared = true)
    private fun damageAction() = combinationParser(
        Action.new("属性系统", "攻击目标", "damage", true)
            .description("攻击目标，支持接入属性系统")
            .addEntry("攻击数值", Type.DOUBLE)
            .addEntry("攻击是否接入属性系统", Type.BOOLEAN, true, default = "false")
            .addContainerEntry("攻击目标")
            .addEntry("攻击来源", Type.CONTAINER, true, "@self", head = "source")
            .addEntry("攻击类型", Type.STRING, true, "PHYSICS", head = "type")
    ) {
        it.group(
            double(),
            bool().option().defaultsTo(false),
            theyContainer(),
            command("source", then = container()).option(),
            command("type", then = text()).option()
        ).apply(it) { damage, attribute, container, source, type ->
            now {
                val sources = source.orElse(self())
                val damageType = type?.uppercase()?.let { it1 -> DamageType.valueOf(it1) } ?: DamageType.PHYSICS
                if (attribute) {
                    container!!.forEachInstance<ITargetEntity<*>> { target ->
                        target.entity.getBukkitLivingEntity()?.let { entity ->
                            IAttributeBridge.INSTANCE.damage(sources.firstInstance<PlayerTarget>().player, entity, damage, damageType)
                        }
                    }
                } else {
                    container!!.forEachInstance<ITargetEntity<*>> { target ->
                        target.entity.getBukkitLivingEntity()?.let { entity ->
                            Default.damage(sources.firstInstance<PlayerTarget>().player, entity, damage, damageType)
                        }
                    }
                }
            }
        }
    }


}