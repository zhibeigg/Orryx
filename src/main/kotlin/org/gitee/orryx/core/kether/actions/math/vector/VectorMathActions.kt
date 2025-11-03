package org.gitee.orryx.core.kether.actions.math.vector

import org.gitee.orryx.api.adapters.IVector
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import org.joml.Matrix3d
import org.joml.Vector3d
import org.joml.Vector3dc
import taboolib.common.OpenResult
import taboolib.common5.cdouble
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object VectorMathActions {

    init {
        KetherLoader.registerProperty(propertyIVector(), Vector3dc::class.java, false)
        KetherLoader.registerProperty(propertyVector(), IVector::class.java, false)
    }

    fun propertyIVector(): ERROR = object : ScriptProperty<Vector3dc>("Vector3dc.operator") {

        override fun read(instance: Vector3dc, key: String): OpenResult {
            return when (key) {
                "x" -> OpenResult.successful(instance.x())
                "y" -> OpenResult.successful(instance.y())
                "z" -> OpenResult.successful(instance.z())
                "size", "length" -> OpenResult.successful(instance.length())
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: Vector3dc, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }

    fun propertyVector(): ERROR = object : ScriptProperty<IVector>("vector.operator") {

        override fun read(instance: IVector, key: String): OpenResult {
            return when (key) {
                "x" -> OpenResult.successful(instance.x())
                "y" -> OpenResult.successful(instance.y())
                "z" -> OpenResult.successful(instance.z())
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: IVector, key: String, value: Any?): OpenResult {
            return when (key) {
                "x" -> {
                    instance.joml.x = value.cdouble
                    OpenResult.successful(instance.x())
                }

                "y" -> {
                    instance.joml.y = value.cdouble
                    OpenResult.successful(instance.y())
                }

                "z" -> {
                    instance.joml.z = value.cdouble
                    OpenResult.successful(instance.z())
                }

                else -> OpenResult.failed()
            }
        }
    }

    @KetherParser(["vector"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionVector() = scriptParser(
        Action.new("Math数学运算", "创建向量", "vector", true)
            .description("创建向量")
            .addEntry("创建标识符", Type.SYMBOL, true, "create", "create/new")
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
        Action.new("Math数学运算", "向量长度", "vector", true)
            .description("向量长度")
            .addEntry("长度标识符", Type.SYMBOL, head = "length")
            .addEntry("向量", Type.VECTOR, false)
            .result("标准化的向量", Type.VECTOR),
        Action.new("Math数学运算", "世界原点向量(0向量)", "vector", true)
            .description("世界原点向量(0向量)")
            .addEntry("0向量标识符", Type.SYMBOL, head = "center")
            .result("0向量", Type.VECTOR),
        Action.new("Math数学运算", "原点向量", "vector", true)
            .description("原点向量")
            .addEntry("原点标识符", Type.SYMBOL, head = "origin")
            .result("origin原点向量", Type.VECTOR),
        Action.new("Math数学运算", "原点的视角向量", "vector", true)
            .description("原点的视角向量")
            .addEntry("原点视角标识符", Type.SYMBOL, head = "eye")
            .addEntry("偏移yaw和pitch(os 90.0 90.0)", Type.DOUBLE, head = "offset/os")
            .result("origin原点的视角向量", Type.VECTOR),
        Action.new("Math数学运算", "根据法线反射向量", "vector", true)
            .description("根据法线反射向量")
            .addEntry("反射占位符", Type.SYMBOL, head = "reflect")
            .addEntry("向量", Type.VECTOR)
            .addEntry("法线向量", Type.VECTOR)
            .addDest(Type.VECTOR, optional = true)
            .result("反射后向量", Type.VECTOR),
        Action.new("Math数学运算", "向量靠近另一向量", "vector", true)
            .description("使向量靠近另一向量")
            .addEntry("靠近占位符", Type.SYMBOL, head = "closer")
            .addEntry("移动向量", Type.VECTOR)
            .addEntry("目标向量", Type.VECTOR)
            .addEntry("距离", Type.DOUBLE)
            .addDest(Type.VECTOR, optional = true)
            .result("移动后的向量", Type.VECTOR),
        Action.new("Math数学运算", "向量远离另一向量", "vector", true)
            .description("使向量远离另一向量")
            .addEntry("远离占位符", Type.SYMBOL, head = "further")
            .addEntry("移动向量", Type.VECTOR)
            .addEntry("目标向量", Type.VECTOR)
            .addEntry("距离", Type.DOUBLE)
            .addDest(Type.VECTOR, optional = true)
            .result("移动后的向量", Type.VECTOR)
    ) {
        it.switch {
            case("create", "new") { create(it) }
            case("add") { add(it) }
            case("sub") { sub(it) }
            case("cross") { cross(it) }
            case("dot") { dot(it) }
            case("mul") { mul(it) }
            case("angle") { angle(it) }
            case("distance") { distance(it) }
            case("negate") { negate(it) }
            case("normalize") { normalize(it) }
            case("length") { length(it) }
            case("reflect") { reflect(it) }
            case("closer") { closer(it) }
            case("further") { further(it) }
            case("center") {
                actionFuture { future ->
                    future.complete(AbstractVector())
                }
            }
            case("origin") {
                actionNow {
                    script().getParameter().origin?.location?.toVector()?.let { v -> AbstractVector(v.x, v.y, v.z) } ?: AbstractVector(0.0, 0.0, 0.0)
                }
            }
            case("eye") {
                val (yaw, pitch) = try {
                    it.mark()
                    it.expects("offset", "os")
                    it.nextParsedAction() to it.nextParsedAction()
                } catch (_: Throwable) {
                    it.reset()
                    literalAction(0) to literalAction(0)
                }

                actionFuture { future ->
                    run(yaw).double { yaw ->
                        run(pitch).double { pitch ->
                            future.complete(
                                script().getParameter().origin?.location?.direction?.let { v ->
                                    val joml = Vector3d(v.x, 0.0, v.z)
                                    val z = joml.cross(0.0, 1.0, 0.0)
                                    val y = z.cross(Vector3d(v.x, v.y, v.z), Vector3d())
                                    val matrix = Matrix3d().rotate(Math.toRadians(yaw), y).rotate(Math.toRadians(pitch), z)
                                    AbstractVector(v.x, v.y, v.z).apply {
                                        mul(matrix, this.joml)
                                    }
                                }
                            )
                        }
                    }
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
                        Vector3d()
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

        return actionFuture { future ->
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

        return actionFuture { future ->
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
        val length = reader.nextHeadAction("length", def = 1.0)
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

    private fun length(reader: QuestReader): ScriptAction<Any?> {

        val a = reader.nextParsedAction()

        return actionFuture { future ->
            run(a).vector { a ->
                future.complete(a.length())
            }
        }
    }

    private fun reflect(reader: QuestReader): ScriptAction<Any?> {

        val a = reader.nextParsedAction()
        val normal = reader.nextParsedAction()
        val dest = reader.nextDest()

        return actionFuture { future ->
            run(a).vector { a ->
                run(normal).vector { normal ->
                    destVector(dest) {
                        future.complete(a.reflect(normal, it.joml))
                    }
                }
            }
        }
    }

    private fun closer(reader: QuestReader): ScriptAction<Any?> {

        val a = reader.nextParsedAction()
        val to = reader.nextParsedAction()
        val length = reader.nextParsedAction()
        val dest = reader.nextDest()

        return actionFuture { future ->
            run(a).vector { a ->
                run(to).vector { to ->
                    run(length).double { length ->
                        destVector(dest) {
                            val dir = to.sub(a, Vector3d()).normalize(length)
                            future.complete(a.add(dir, it.joml))
                        }
                    }
                }
            }
        }
    }

    private fun further(reader: QuestReader): ScriptAction<Any?> {

        val a = reader.nextParsedAction()
        val to = reader.nextParsedAction()
        val length = reader.nextParsedAction()
        val dest = reader.nextDest()

        return actionFuture { future ->
            run(a).vector { a ->
                run(to).vector { to ->
                    run(length).double { length ->
                        destVector(dest) {
                            val dir = a.sub(to, Vector3d()).normalize(length)
                            future.complete(a.add(dir, it.joml))
                        }
                    }
                }
            }
        }
    }
}