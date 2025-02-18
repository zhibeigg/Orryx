package org.gitee.orryx.core.kether.actions.math.vector

import org.bukkit.util.Vector
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import org.joml.Vector3d
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object VectorMathActions {

    @KetherParser(["vector"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionVector() = scriptParser(
        arrayOf(
            Action.new("Math数学运算", "创建向量", "vector", true)
                .description("创建向量")
                .addEntry("创建标识符", Type.SYMBOL, true, "create", "create")
                .addEntry("X", Type.DOUBLE, false)
                .addEntry("Y", Type.DOUBLE, false)
                .addEntry("Z", Type.DOUBLE, false)
                .result("创建的向量", Type.VECTOR),
            Action.new("Math数学运算", "向量相加", "vector", true)
                .description("向量相加")
                .addEntry("相加标识符", Type.SYMBOL, head = "add")
                .addEntry("加号前的向量A", Type.VECTOR, false)
                .addEntry("加号后的向量B", Type.VECTOR, false)
                .addDest(Type.VECTOR, optional = true)
                .result("相加结果的向量", Type.VECTOR),
            Action.new("Math数学运算", "向量相减", "vector", true)
                .description("向量相减")
                .addEntry("相减标识符", Type.SYMBOL, head = "sub")
                .addEntry("减号前的向量A", Type.VECTOR, false)
                .addEntry("减号后的向量B", Type.VECTOR, false)
                .addDest(Type.VECTOR, optional = true)
                .result("相减结果的向量", Type.VECTOR),
            Action.new("Math数学运算", "叉乘向量", "vector", true)
                .description("叉乘向量")
                .addEntry("叉乘标识符", Type.SYMBOL, head = "cross")
                .addEntry("叉乘号前的向量A", Type.VECTOR, false)
                .addEntry("叉乘号后的向量B", Type.VECTOR, false)
                .addDest(Type.VECTOR, optional = true)
                .result("叉乘结果的向量", Type.VECTOR),
            Action.new("Math数学运算", "点乘向量", "vector", true)
                .description("点乘向量")
                .addEntry("点乘标识符", Type.SYMBOL, head = "dot")
                .addEntry("点乘号前的向量A", Type.VECTOR, false)
                .addEntry("点乘号后的向量B", Type.VECTOR, false)
                .result("点乘结果", Type.VECTOR),
            Action.new("Math数学运算", "数乘向量", "vector", true)
                .description("数乘向量")
                .addEntry("数乘标识符", Type.SYMBOL, head = "mul")
                .addEntry("向量", Type.VECTOR, false)
                .addEntry("数字", Type.DOUBLE, false)
                .addDest(Type.VECTOR, optional = true)
                .result("数乘结果", Type.VECTOR),
            Action.new("Math数学运算", "向量夹角", "vector", true)
                .description("向量A与向量B的夹角")
                .addEntry("夹角标识符", Type.SYMBOL, head = "angle")
                .addEntry("向量A", Type.VECTOR, false)
                .addEntry("向量B", Type.VECTOR, false)
                .result("向量夹角角度值", Type.DOUBLE),
            Action.new("Math数学运算", "向量距离", "vector", true)
                .description("向量A与向量B的距离")
                .addEntry("距离标识符", Type.SYMBOL, head = "distance")
                .addEntry("向量A", Type.VECTOR, false)
                .addEntry("向量B", Type.VECTOR, false)
                .result("向量距离", Type.DOUBLE),
            Action.new("Math数学运算", "反转向量", "vector", true)
                .description("反转向量")
                .addEntry("反转标识符", Type.SYMBOL, head = "negate")
                .addEntry("向量", Type.VECTOR, false)
                .addDest(Type.VECTOR, optional = true)
                .result("反转后向量", Type.VECTOR),
            Action.new("Math数学运算", "归一化", "vector", true)
                .description("标准化向量")
                .addEntry("标准化标识符", Type.SYMBOL, head = "normalize")
                .addEntry("需要标准化的向量", Type.VECTOR, false)
                .addEntry("标准化的长度", Type.DOUBLE, true, "1.0", "length")
                .addDest(Type.VECTOR, optional = true)
                .result("标准化的向量", Type.VECTOR),
            Action.new("Math数学运算", "世界原点向量(0向量)", "vector", true)
                .description("世界原点向量(0向量)")
                .addEntry("0向量标识符", Type.SYMBOL, head = "center")
                .result("0向量", Type.VECTOR),
            Action.new("Math数学运算", "原点向量", "vector", true)
                .description("原点向量")
                .addEntry("原点标识符", Type.SYMBOL, head = "origin")
                .result("origin原点向量", Type.VECTOR),
        )
    ) {
        it.switch {
            case("create") { create(it) }
            case("add") { add(it) }
            case("sub") { sub(it) }
            case("cross") { cross(it) }
            case("dot") { dot(it) }
            case("mul") { mul(it) }
            case("angle") { angle(it) }
            case("distance") { distance(it) }
            case("negate") { negate(it) }
            case("normalize") { normalize(it) }
            case("center") {
                actionFuture { future ->
                    future.complete(AbstractVector())
                }
            }
            case("origin") {
                actionFuture { future ->
                    val vector = script().getParameter().origin?.location?.toVector() ?: Vector(0, 0, 0)
                    val vector3d = Vector3d(vector.x, vector.y, vector.z)
                    future.complete(vector3d.abstract())
                }
            }
            other { create(it) }
        }
    }

    private fun create(reader: QuestReader): ScriptAction<Any?> {
        val x = reader.nextParsedAction()
        val y = reader.nextParsedAction()
        val z = reader.nextParsedAction()
        return actionFuture {
            run(x).double { x ->
                run(y).double { y ->
                    run(z).double { z ->
                        it.complete(Vector3d(x, y, z).abstract())
                    }
                }
            }
        }
    }

    private fun add(reader: QuestReader): ScriptAction<Any?> {
        val a = reader.nextParsedAction()
        val b = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(a).vector { a ->
                run(b).vector { b ->
                    destVector(dest) {
                        future.complete(a.add(b, it.joml))
                    }
                }
            }
        }
    }

    private fun sub(reader: QuestReader): ScriptAction<Any?> {
        val a = reader.nextParsedAction()
        val b = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(a).vector { a ->
                run(b).vector { b ->
                    destVector(dest) {
                        future.complete(a.sub(b, it.joml))
                    }
                }
            }
        }
    }

    private fun cross(reader: QuestReader): ScriptAction<Any?> {
        val a = reader.nextParsedAction()
        val b = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(a).vector { a ->
                run(b).vector { b ->
                    destVector(dest) {
                        future.complete(a.cross(b, it.joml))
                    }
                }
            }
        }
    }

    private fun dot(reader: QuestReader): ScriptAction<Any?> {
        val a = reader.nextParsedAction()
        val b = reader.nextParsedAction()
        return actionFuture {future ->
            run(a).vector { a ->
                run(b).vector { b ->
                    future.complete(a.dot(b))
                }
            }
        }
    }

    private fun mul(reader: QuestReader): ScriptAction<Any?> {
        val v = reader.nextParsedAction()
        val scale = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture {future ->
            run(v).vector { v ->
                run(scale).double { scale ->
                    destVector(dest) {
                        future.complete(v.mul(scale, it.joml))
                    }
                }
            }
        }
    }

    private fun angle(reader: QuestReader): ScriptAction<Any?> {
        val a = reader.nextParsedAction()
        val b = reader.nextParsedAction()
        return actionFuture {
            run(a).vector { a ->
                run(b).vector { b ->
                    it.complete(Math.toDegrees(a.angle(b)))
                }
            }
        }
    }

    private fun distance(reader: QuestReader): ScriptAction<Any?> {
        val a = reader.nextParsedAction()
        val b = reader.nextParsedAction()
        return actionFuture {
            run(a).vector { a ->
                run(b).vector { b ->
                    it.complete(a.distance(b))
                }
            }
        }
    }

    private fun negate(reader: QuestReader): ScriptAction<Any?> {
        val a = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(a).vector { a ->
                destVector(dest) {
                    future.complete(a.negate(it.joml))
                }
            }
        }
    }

    private fun normalize(reader: QuestReader): ScriptAction<Any?> {
        val a = reader.nextParsedAction()
        val length = reader.nextHeadAction("length", 1.0)
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(a).vector { a ->
                run(length).double { length ->
                    destVector(dest) {
                        future.complete(a.normalize(length, it.joml))
                    }
                }
            }
        }
    }

}