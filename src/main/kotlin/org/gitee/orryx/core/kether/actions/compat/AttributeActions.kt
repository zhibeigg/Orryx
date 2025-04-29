package org.gitee.orryx.core.kether.actions.compat

import org.bukkit.entity.LivingEntity
import org.gitee.orryx.compat.IAttributeBridge
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.container
import org.gitee.orryx.utils.forEachInstance
import org.gitee.orryx.utils.nextTheyContainerOrNull
import org.gitee.orryx.utils.scriptParser
import org.gitee.orryx.utils.self
import taboolib.module.kether.KetherParser
import taboolib.module.kether.actionNow
import taboolib.module.kether.long
import taboolib.module.kether.run
import taboolib.module.kether.str
import taboolib.module.kether.switch

object AttributeActions {

    @KetherParser(["attribute"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun attribute() = scriptParser(
        Action.new("Attribute属性", "添加临时属性", "attribute", true)
            .description("添加临时属性")
            .addEntry("识别key", Type.STRING)
            .addEntry("属性类似：(物理攻击: +10, 生命上限: +10)", Type.STRING)
            .addEntry("时长(-1为永久)", Type.LONG)
            .addContainerEntry("添加目标", true, "@self"),
        Action.new("Attribute属性", "移除临时属性", "attribute", true)
            .description("移除临时属性")
            .addEntry("识别key", Type.STRING)
            .addContainerEntry("添加目标", true, "@self"),
        Action.new("Attribute属性", "更新实体属性", "attribute", true)
            .description("更新实体属性")
            .addContainerEntry("更新目标", true, "@self")
    ) {
        it.switch {
            case("add") {
                val key = it.nextToken()
                val attribute = it.nextParsedAction()
                val timeout = it.nextParsedAction()
                val they = it.nextTheyContainerOrNull()
                val instance = IAttributeBridge.INSTANCE
                actionNow {
                    run(attribute).str { attribute ->
                        val attributes = attribute.split(",")
                        run(timeout).long { timeout ->
                            container(they, self()) { container ->
                                container.forEachInstance<ITargetEntity<LivingEntity>> { targetEntity ->
                                    instance.addAttribute(targetEntity.getSource(), key, attributes, timeout)
                                }
                            }
                        }
                    }
                }
            }
            case("remove") {
                val key = it.nextToken()
                val they = it.nextTheyContainerOrNull()
                val instance = IAttributeBridge.INSTANCE
                actionNow {
                    container(they, self()) { container ->
                        container.forEachInstance<ITargetEntity<LivingEntity>> { targetEntity ->
                            instance.removeAttribute(targetEntity.getSource(), key)
                        }
                    }
                }
            }
            case("update") {
                val they = it.nextTheyContainerOrNull()
                val instance = IAttributeBridge.INSTANCE
                actionNow {
                    container(they, self()) { container ->
                        container.forEachInstance<ITargetEntity<LivingEntity>> { targetEntity ->
                            instance.update(targetEntity.getSource())
                        }
                    }
                }
            }
        }
    }
}