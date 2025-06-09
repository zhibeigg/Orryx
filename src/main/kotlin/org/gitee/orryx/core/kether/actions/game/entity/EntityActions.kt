package org.gitee.orryx.core.kether.actions.game.entity

import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.gitee.orryx.api.adapters.entity.AbstractAdyeshachEntity
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.kether.ScriptManager.addOrryxCloseable
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object EntityActions {

    @KetherParser(["entity"], namespace = ORRYX_NAMESPACE)
    private fun entityParser() = scriptParser(
        Action.new("Entity实体操作", "获取实体参数", "entity", true)
            .description("获取实体参数")
            .addEntry("实体参数key", Type.STRING, default = "uuid")
            .addContainerEntry("被获取的实体", optional = true, default = "@self")
            .result("指定参数", Type.ANY),
        Action.new("Entity实体操作", "生成原版实体", "entity", true)
            .description("生成原版实体，脚本运行时间必须大于实体存在时间，若提前停止将会直接回收实体")
            .addEntry("生成标识符", Type.SYMBOL, head = "spawn")
            .addEntry("实体名", Type.STRING)
            .addEntry("实体类型", Type.STRING)
            .addEntry("实体血量", Type.DOUBLE, optional = true, head = "health")
            .addEntry("实体冲量", Type.VECTOR, optional = true, head = "vector")
            .addEntry("实体是否具有重力", Type.BOOLEAN, optional = true, head = "gravity", default = "true")
            .addEntry("实体存在时间(0为永久)", Type.LONG, optional = true, head = "timeout", default = "0")
            .addContainerEntry("实体生成位置", optional = true, default = "@self")
            .result("生成的实体列表", Type.CONTAINER),
        Action.new("Entity实体操作", "生成Ady实体", "entity", true)
            .description("生成Ady实体，脚本运行时间必须大于实体存在时间，若提前停止将会直接回收实体")
            .addEntry("ady标识符", Type.SYMBOL, head = "ady")
            .addEntry("实体名", Type.STRING)
            .addEntry("实体类型", Type.STRING)
            .addEntry("实体冲量", Type.VECTOR, optional = true, head = "vector")
            .addEntry("实体是否具有重力", Type.BOOLEAN, optional = true, head = "gravity", default = "true")
            .addEntry("实体存在时间(0为永久)", Type.LONG, optional = true, head = "timeout", default = "0")
            .addContainerEntry("实体生成位置", optional = true, default = "@self")
            .addContainerEntry("能看到实体的玩家（默认私有）", optional = true, head = "viewer", default = "@self")
            .result("生成的实体列表", Type.CONTAINER),
        Action.new("Entity实体操作", "移除实体", "entity", true)
            .description("移除实体")
            .addEntry("移除标识符", Type.SYMBOL, head = "remove/destroy")
            .addContainerEntry("实体", optional = false)
    ) {
        it.switch {
            case("spawn") { spawn(this) }
            case("ady") { adySpawn(this) }
            case("remove", "destroy") { remove(this) }
            other { fieldGet(this) }
        }
    }

    private fun spawn(reader: QuestReader): ScriptAction<Any?> {
        val name = reader.nextParsedAction()
        val type = reader.nextParsedAction()
        val health = reader.nextHeadAction("health", def = 0.0)
        val vector = reader.nextHeadAction("vector", def = AbstractVector())
        val gravity = reader.nextHeadAction("gravity", def = true)
        val timeout = reader.nextHeadAction("timeout", def = 0.0)
        val they = reader.nextTheyContainerOrNull()
        return actionFuture { future ->
            run(name).str { name ->
                run(type).str { type ->
                    run(health).double { health ->
                        run(vector).vector { vector ->
                            run(gravity).bool { gravity ->
                                run(timeout).long { timeout ->
                                    containerOrSelf(they) {
                                        val builder =
                                            EntityBuilder()
                                                .name(name)
                                                .type(EntityType.valueOf(type.uppercase()))
                                                .health(health)
                                                .vector(vector)
                                                .gravity(gravity)
                                                .timeout(timeout)

                                        val locations = it.mapNotNullInstance<ITargetLocation<*>, Location> { target ->
                                            target.location
                                        }

                                        ensureSync {
                                            Container(builder.build(locations).mapTo(linkedSetOf()) { entity ->
                                                entity as AbstractBukkitEntity
                                            })
                                        }.thenApply { container ->
                                            addOrryxCloseable(builder.removed) {
                                                fun clean() {
                                                    builder.task.cancel()
                                                    container.forEachInstance<ITargetEntity<*>> { target ->
                                                        if (target.entity.isValid) {
                                                            target.entity.remove()
                                                        }
                                                    }
                                                }
                                                ensureSync { clean() }
                                            }
                                            future.complete(it)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun adySpawn(reader: QuestReader): ScriptAction<Any?> {
        val name = reader.nextParsedAction()
        val type = reader.nextParsedAction()
        val vector = reader.nextHeadAction("vector", def = AbstractVector())
        val gravity = reader.nextHeadAction("gravity", def = true)
        val timeout = reader.nextHeadAction("timeout", def = 0.0)
        val viewer = reader.nextHeadActionOrNull("viewer")
        val they = reader.nextTheyContainerOrNull()
        return actionFuture { future ->
            run(name).str { name ->
                run(type).str { type ->
                    run(vector).vector { vector ->
                        run(gravity).bool { gravity ->
                            run(timeout).long { timeout ->
                                containerOrSelf(viewer) { viewer ->
                                    containerOrSelf(they) {
                                        val players = viewer.get<PlayerTarget>()
                                        val builder = EntityBuilder()
                                            .name(name)
                                            .type(EntityType.valueOf(type.uppercase()))
                                            .vector(vector)
                                            .gravity(gravity)
                                            .timeout(timeout)
                                            .private(players.size == 1)

                                        val locations = it.mapNotNullInstance<ITargetLocation<*>, Location> { target ->
                                            target.location
                                        }

                                        ensureSync {
                                            Container(builder.build(locations, players.map { playerTarget -> playerTarget.getSource() }, true).mapTo(linkedSetOf()) { entity ->
                                                entity as AbstractAdyeshachEntity
                                            })
                                        }.thenAccept { container ->
                                            addOrryxCloseable(builder.removed) {
                                                fun clean() {
                                                    builder.task.cancel()
                                                    container.forEachInstance<ITargetEntity<*>> { target ->
                                                        if (target.entity.isValid) {
                                                            target.entity.remove()
                                                        }
                                                    }
                                                }
                                                ensureSync { clean() }
                                            }
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
    }

    private fun remove(reader: QuestReader): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()
        return actionFuture { future ->
            ensureSync {
                containerOrSelf(they) {
                    it.forEachInstance<ITargetEntity<*>> { target ->
                        if (target.entity.isValid) target.entity.remove()
                    }
                    future.complete(null)
                }
            }
        }
    }

    private fun fieldGet(reader: QuestReader): ScriptAction<Any?> {
        val key = reader.nextToken().uppercase()
        val they = reader.nextTheyContainerOrNull()
        return actionFuture { future ->
            containerOrSelf(they) {
                val entity = it.firstInstance<ITargetEntity<*>>().entity
                future.complete(EntityField.valueOf(key).get(entity))
            }
        }
    }
}