package org.gitee.orryx.core.kether.actions.compat

import org.bukkit.entity.LivingEntity
import org.gitee.orryx.compat.IAttributeBridge
import org.gitee.orryx.compat.attributeplus.AttributePlusBridge
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.KetherParser

object AttributePlus3Actions {

    @KetherParser(["apAttack"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun apAttack() = combinationParser(
        Action.new("AttributePlus", "ap3攻击", "apAttack", true)
            .description("ap3攻击")
            .addEntry("重置属性", Type.BOOLEAN)
            .addEntry("属性(用,分割)", Type.STRING, true, head = "attributes")
            .addContainerEntry("防御者defender")
            .addEntry("攻击来源", Type.CONTAINER, true, "@self", head = "source")
    ) {
        it.group(
            bool(),
            command("attributes", then = text()).option(),
            theyContainer(false),
            command("source", then = container()).option(),
        ).apply(it) { reset, attributes, they, source ->
            now {
                val sources = source.orElse(self())
                val attacker = sources.firstInstance<ITargetEntity<LivingEntity>>().getSource()
                val instance = IAttributeBridge.INSTANCE as? AttributePlusBridge ?: return@now
                val attributes = attributes?.split(",") ?: emptyList()

                ensureSync {
                    they!!.forEachInstance<ITargetEntity<*>> { target ->
                        target.entity.getBukkitLivingEntity()?.let { entity ->
                            instance.apAttack(attacker, entity, reset, attributes)
                        }
                    }
                }
            }
        }
    }
}