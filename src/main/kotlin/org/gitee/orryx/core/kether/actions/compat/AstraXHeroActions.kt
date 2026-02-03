package org.gitee.orryx.core.kether.actions.compat

import cn.bukkitmc.hero.FightAPI
import cn.bukkitmc.hero.module.fight.FightData
import org.bukkit.entity.LivingEntity
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.KetherParser

object AstraXHeroActions {

    @KetherParser(["axhDamage"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun axhDamage() = combinationParser(
        Action.new("AstraXHero", "AstraXHero攻击", "axhDamage", true)
            .description("AstraXHero攻击")
            .addEntry("是否造成伤害", Type.BOOLEAN, true, "true", head = "doDamage")
            .addEntry("战斗变量(格式: var1=1;var2=2)", Type.STRING, head = "variable")
            .addContainerEntry("防御者defender")
            .addEntry("攻击来源", Type.CONTAINER, true, "@self", head = "source")
    ) {
        it.group(
            bool(),
            text(),
            theyContainer(optional = false),
            command("source", then = container()).option()
        ).apply(it) { doDamage, variable, they, source ->
            now {
                val sources = source.orElse(self())
                val attacker = sources.firstInstance<ITargetEntity<LivingEntity>>().getSource()
                val variables = mutableMapOf<String, Any>().also { map ->
                    // var1=1;var2=2 -> [var1=1, var2=2]
                    val kvs = variable.split(";")
                    for (line in kvs) {
                        // [var1=1] -> [var1, 1]
                        val kv = line.split("=")
                        if (kv.size == 2) {
                            // [var1, 1] -> {var1=1}
                            map[kv[0]] = kv[1]
                        }
                    }
                }
                ensureSync {
                    they!!.forEachInstance<ITargetEntity<*>> { target ->
                        target.entity.getBukkitLivingEntity()?.let { defender ->
                            val data = FightData(attacker, defender) { fd ->
                                fd["orryx"] = true
                                fd.putAll(variables)
                            }
                            FightAPI.runFight(data, doDamage)
                        }
                    }
                }
            }
        }
    }
}