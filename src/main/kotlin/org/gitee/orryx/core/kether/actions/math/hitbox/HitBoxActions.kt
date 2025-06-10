package org.gitee.orryx.core.kether.actions.math.hitbox

import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.api.collider.local.ILocalCollider
import org.gitee.orryx.api.collider.local.ILocalComposite
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.local.*
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import org.joml.Quaterniond
import org.joml.Vector3d
import taboolib.common.platform.function.info
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object HitBoxActions {

    @KetherParser(["hitbox"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionHitbox() = scriptParser(
        Action.new("Hitbox碰撞箱", "创建AABB碰撞箱", "hitbox", true)
            .description("创建AABB碰撞箱")
            .addEntry("aabb标识符", Type.SYMBOL, false, head = "aabb")
            .addEntry("宽度", Type.DOUBLE, false)
            .addEntry("高度", Type.DOUBLE, false)
            .addContainerEntry("绑定的目标", true, default = "@self")
            .addEntry("偏移", Type.VECTOR, true, default = "null", head = "offset")
            .result("创建的碰撞箱", Type.HITBOX),
        Action.new("Hitbox碰撞箱", "创建胶囊体碰撞箱", "hitbox", true)
            .description("创建胶囊体碰撞箱")
            .addEntry("capsule标识符", Type.SYMBOL, false, head = "capsule")
            .addEntry("半径", Type.DOUBLE, false)
            .addEntry("高度", Type.DOUBLE, false)
            .addContainerEntry("绑定的目标", true, default = "@self")
            .addEntry("偏移", Type.VECTOR, true, default = "null", head = "offset")
            .addEntry("旋转", Type.QUATERNION, true, default = "null", head = "rotate")
            .result("创建的碰撞箱", Type.HITBOX),
        Action.new("Hitbox碰撞箱", "创建OBB碰撞箱", "hitbox", true)
            .description("创建OBB碰撞箱")
            .addEntry("obb标识符", Type.SYMBOL, false, head = "obb")
            .addEntry("宽度", Type.DOUBLE, false)
            .addEntry("长度", Type.DOUBLE, false)
            .addEntry("高度", Type.DOUBLE, false)
            .addContainerEntry("绑定的目标", true, default = "@self")
            .addEntry("偏移", Type.VECTOR, true, default = "null", head = "offset")
            .addEntry("旋转", Type.QUATERNION, true, default = "null", head = "rotate")
            .result("创建的碰撞箱", Type.HITBOX),
        Action.new("Hitbox碰撞箱", "创建射线碰撞箱", "hitbox", true)
            .description("创建射线碰撞箱")
            .addEntry("射线标识符", Type.SYMBOL, false, head = "ray")
            .addEntry("长度", Type.DOUBLE, false)
            .addEntry("方向向量", Type.VECTOR, false)
            .addContainerEntry("绑定的目标", true, default = "@self")
            .addEntry("偏移", Type.VECTOR, true, default = "null", head = "offset")
            .result("创建的碰撞箱", Type.HITBOX),
        Action.new("Hitbox碰撞箱", "创建球体碰撞箱", "hitbox", true)
            .description("创建球体碰撞箱")
            .addEntry("球体标识符", Type.SYMBOL, false, head = "sphere")
            .addEntry("半径", Type.DOUBLE, false)
            .addContainerEntry("绑定的目标", true, default = "@self")
            .addEntry("偏移", Type.VECTOR, true, default = "null", head = "offset")
            .result("创建的碰撞箱", Type.HITBOX),
        Action.new("Hitbox碰撞箱", "创建复合体碰撞箱", "hitbox", true)
            .description("创建复合体碰撞箱")
            .addEntry("复合体标识符", Type.SYMBOL, false, head = "composite")
            .addContainerEntry("绑定的目标", true, default = "@self")
            .addEntry("偏移", Type.VECTOR, true, default = "null", head = "offset")
            .addEntry("旋转", Type.QUATERNION, true, default = "null", head = "rotate")
            .result("创建的碰撞箱", Type.HITBOX),
        Action.new("Hitbox碰撞箱", "更新碰撞箱", "hitbox", true)
            .description("更新碰撞箱")
            .addEntry("更新标识符", Type.SYMBOL, false, head = "update")
            .addEntry("更新的碰撞箱", Type.HITBOX),
        Action.new("Hitbox碰撞箱", "添加复合体中的碰撞箱", "hitbox", true)
            .description("添加复合体中的碰撞箱")
            .addEntry("添加标识符", Type.SYMBOL, false, head = "add")
            .addEntry("复合体碰撞箱", Type.HITBOX)
            .addEntry("创建碰撞箱", Type.HITBOX)
            .result("添加的碰撞箱索引", Type.INT),
        Action.new("Hitbox碰撞箱", "移除复合体中的碰撞箱", "hitbox", true)
            .description("移除复合体中的碰撞箱")
            .addEntry("移除标识符", Type.SYMBOL, false, head = "remove")
            .addEntry("复合体碰撞箱", Type.HITBOX)
            .addEntry("移除位置的索引", Type.INT)
    ) {
        it.switch {
            case("aabb") { createAABB(it) }
            case("capsule") { createCapsule(it) }
            case("obb") { createOBB(it) }
            case("ray") { createRay(it) }
            case("sphere") { createSphere(it) }
            case("composite") { createComposite(it) }
            case("update") { update(it) }
            case("add") { add(it) }
            case("remove") { remove(it) }
        }
    }

    private fun createAABB(reader: QuestReader): ScriptAction<Any?> {

        val width = reader.nextParsedAction()
        val height = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()
        val offset = reader.nextHeadAction("offset", def = AbstractVector())

        return actionFuture { future ->
            run(width).double { width ->
                run(height).double { height ->
                    run(offset).vector { offset ->
                        containerOrSelf(they) { container ->
                            val bind = container.firstInstance<ITargetLocation<*>>().coordinateConverter()
                            val halfExtents = Vector3d(width / 2, height / 2, width / 2)
                            future.complete(LocalAABB<ITargetLocation<*>>(offset.joml, halfExtents, bind))
                        }
                    }
                }
            }
        }
    }

    private fun createCapsule(reader: QuestReader): ScriptAction<Any?> {

        val radius = reader.nextParsedAction()
        val height = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()
        val offset = reader.nextHeadAction("offset", def = AbstractVector())
        val rotate = reader.nextHeadAction("rotate", def = Quaterniond())

        return actionFuture { future ->
            run(radius).double { radius ->
                run(height).double { height ->
                    run(offset).vector { offset ->
                        run(rotate).quaternion { rotate ->
                            containerOrSelf(they) { container ->
                                val bind = container.firstInstance<ITargetLocation<*>>().coordinateConverter()
                                future.complete(LocalCapsule<ITargetLocation<*>>(height, radius, offset.joml, rotate, bind))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createOBB(reader: QuestReader): ScriptAction<Any?> {

        val width = reader.nextParsedAction()
        val length = reader.nextParsedAction()
        val height = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()
        val offset = reader.nextHeadAction("offset", def = AbstractVector())
        val rotate = reader.nextHeadAction("rotate", def = Quaterniond())

        return actionFuture { future ->
            run(width).double { width ->
                run(length).double { length ->
                    run(height).double { height ->
                        run(offset).vector { offset ->
                            run(rotate).quaternion { rotate ->
                                containerOrSelf(they) { container ->
                                    val bind = container.firstInstance<ITargetLocation<*>>().coordinateConverter()
                                    val halfExtents = Vector3d(length / 2, height / 2, width / 2)
                                    future.complete(
                                        LocalOBB<ITargetLocation<*>>(
                                            halfExtents,
                                            offset.joml,
                                            rotate,
                                            bind
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createRay(reader: QuestReader): ScriptAction<Any?> {

        val length = reader.nextParsedAction()
        val direction = reader.nextHeadActionOrNull("direction")!!
        val they = reader.nextTheyContainerOrNull()
        val offset = reader.nextHeadAction("offset", def = AbstractVector())

        return actionFuture { future ->
            run(length).double { length ->
                run(direction).vector { direction ->
                    run(offset).vector { offset ->
                        containerOrSelf(they) { container ->
                            val bind = container.firstInstance<ITargetLocation<*>>().coordinateConverter()
                            future.complete(LocalRay<ITargetLocation<*>>(offset.joml, direction.joml, length, bind))
                        }
                    }
                }
            }
        }
    }

    private fun createSphere(reader: QuestReader): ScriptAction<Any?> {

        val radius = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()
        val offset = reader.nextHeadAction("offset", def = AbstractVector())

        return actionFuture { future ->
            run(radius).double { radius ->
                run(offset).vector { offset ->
                    containerOrSelf(they) { container ->
                        val bind = container.firstInstance<ITargetLocation<*>>().coordinateConverter()
                        future.complete(LocalSphere<ITargetLocation<*>>(offset.joml, radius, bind))
                    }
                }
            }
        }
    }

    private fun createComposite(reader: QuestReader): ScriptAction<Any?> {

        val they = reader.nextTheyContainerOrNull()
        val offset = reader.nextHeadAction("offset", def = AbstractVector())
        val rotate = reader.nextHeadAction("rotate", def = Quaterniond())

        return actionFuture { future ->
            run(offset).vector { offset ->
                run(rotate).quaternion { rotate ->
                    containerOrSelf(they) { container ->
                        val bind = container.firstInstance<ITargetLocation<*>>().coordinateConverter()
                        future.complete(LocalComposite<ITargetLocation<*>, ILocalCollider<ITargetLocation<*>>>(offset.joml, rotate, bind))
                    }
                }
            }
        }
    }

    private fun update(reader: QuestReader): ScriptAction<Any?> {

        val hitbox = reader.nextParsedAction()

        return actionNow {
            run(hitbox).collider { hitbox ->
                hitbox.update()
            }
        }
    }

    private fun add(reader: QuestReader): ScriptAction<Any?> {
        return actionNow {
            info("贼复杂，后面再写")
        }
    }

    private fun remove(reader: QuestReader): ScriptAction<Any?> {

        val hitbox = reader.nextParsedAction()
        val index = reader.nextParsedAction()

        return actionNow {
            run(hitbox).collider { hitbox ->
                run(index).int { index ->
                    if (hitbox is ILocalComposite<*, *>) {
                        hitbox.removeCollider(index)
                    } else {
                        error("只有复合型碰撞箱才能添加碰撞箱")
                    }
                }
            }
        }
    }
}