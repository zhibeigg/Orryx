package org.gitee.orryx.core.kether.actions.game.entity

import org.bukkit.entity.EntityType
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.util.*

object EntityActions {

    @KetherParser(["entity"], namespace = ORRYX_NAMESPACE)
    private fun entityParser() = scriptParser {
        arrayOf(
            Action.new("Entity实体操作", "获取实体参数", "entity", true)
                .description("获取实体参数")
                .addEntry("实体参数key", Type.STRING, default = "uuid")
                .result("指定参数", Type.ANY),
            Action.new("Entity实体操作", "生成原版实体", "entity", true)
                .description("获取实体参数")
                .addEntry("生成标识符", Type.SYMBOL, head = "spawn")
                .addEntry("实体名", Type.STRING)
                .addEntry("实体类型", Type.STRING)
                .addEntry("实体血量", Type.DOUBLE, optional = true, head = "health")
                .addEntry("实体冲量", Type.VECTOR, optional = true, head = "vector")
                .addEntry("实体是否具有重力", Type.BOOLEAN, optional = true, head = "gravity", default = "true")
                .addEntry("实体存在时间(0为永久)", Type.LONG, optional = true, head = "timeout", default = "0")
                .addContainerEntry("实体生成位置", optional = true, default = "@self")
                .result("生成的实体列表", Type.CONTAINER),
        )
        it.switch {
            case("spawn") { spawn(this) }
            other { fieldGet(this) }
        }
    }

    private fun spawn(reader: QuestReader): ScriptAction<Any?> {
        val name = reader.nextParsedAction()
        val type = reader.nextParsedAction()
        val health = reader.nextHeadAction("health", 0.0)
        val vector = reader.nextHeadAction("vector", AbstractVector())
        val gravity = reader.nextHeadAction("gravity", true)
        val timeout = reader.nextHeadAction("timeout", 0.0)
        val they = reader.nextTheyContainer()
        return actionFuture { future ->
            run(name).str { name ->
                run(type).str { type ->
                    run(health).double { health ->
                        run(vector).vector { vector ->
                            run(gravity).bool { gravity ->
                                run(timeout).long { timeout ->
                                    containerOrSelf(they) {
                                        val container = Container(
                                            it.mapNotNullInstance<ITargetLocation<*>, AbstractBukkitEntity> { target ->
                                                EntityBuilder()
                                                    .name(name)
                                                    .type(EntityType.valueOf(type))
                                                    .health(health)
                                                    .vector(vector)
                                                    .gravity(gravity)
                                                    .timeout(timeout)
                                                    .location(target.location)
                                                    .build() as AbstractBukkitEntity
                                            }.toMutableSet()
                                        )
                                        val list = container.targets.mapNotNull { iTarget -> (iTarget.getSource() as? AbstractBukkitEntity) }
                                        addClosable(AutoCloseable { list.forEach { entity ->
                                            EntityBuilder.taskMap.remove(entity.uniqueId)?.cancel()
                                            if (entity.isValid) {
                                                entity.remove()
                                            }
                                        } })
                                        future.complete(container)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fieldGet(reader: QuestReader): ScriptAction<Any?> {
        return try {
            reader.mark()
            val expect = reader.expects(*EntityField.fields().toTypedArray())
            val they = reader.nextTheyContainer()
            actionFuture { future ->
                containerOrSelf(they) {
                    val entity = it.firstInstance<ITargetEntity<*>>().entity
                    future.complete(EntityField.valueOf(expect.uppercase(Locale.getDefault())).get(entity))
                }
            }
        } catch (_: Throwable) {
            reader.reset()
            error("entity field not found")
        }
    }

}