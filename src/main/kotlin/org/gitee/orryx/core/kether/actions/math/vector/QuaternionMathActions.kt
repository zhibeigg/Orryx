package org.gitee.orryx.core.kether.actions.math.vector

import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import org.joml.Quaterniond
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object QuaternionMathActions {

    @KetherParser(["quaternion"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionQuaternion() = scriptParser(
        Action.new("Math数学运算", "创建单位四元数", "quaternion", true)
            .description("创建单位四元数")
            .addEntry("创建单位四元数标识符", Type.SYMBOL, true, "identity", "identity")
            .result("创建的单位四元数", Type.QUATERNION),
        Action.new("Math数学运算", "创建四元数", "quaternion", true)
            .description("创建四元数")
            .addEntry("创建标识符", Type.SYMBOL, head = "create")
            .addEntry("x", Type.DOUBLE)
            .addEntry("y", Type.DOUBLE)
            .addEntry("z", Type.DOUBLE)
            .addEntry("w", Type.DOUBLE)
            .result("创建的矩阵", Type.QUATERNION),
        Action.new("Math数学运算", "rotateX四元数绕X轴旋转", "quaternion", true)
            .description("四元数绕X轴旋转")
            .addEntry("X轴旋转标识符", Type.SYMBOL, head = "rotateX")
            .addEntry("被旋转的四元数", Type.QUATERNION)
            .addEntry("旋转角度", Type.DOUBLE)
            .addDest(Type.QUATERNION, optional = true)
            .result("旋转四元数", Type.QUATERNION),
        Action.new("Math数学运算", "rotateY四元数绕Y轴旋转", "quaternion", true)
            .description("四元数绕Y轴旋转")
            .addEntry("Y轴旋转标识符", Type.SYMBOL, head = "rotateY")
            .addEntry("被旋转的四元数", Type.QUATERNION)
            .addEntry("旋转角度", Type.DOUBLE)
            .addDest(Type.QUATERNION, optional = true)
            .result("旋转四元数", Type.QUATERNION),
        Action.new("Math数学运算", "rotateZ四元数绕Z轴旋转", "quaternion", true)
            .description("四元数绕Z轴旋转")
            .addEntry("Z轴旋转标识符", Type.SYMBOL, head = "rotateZ")
            .addEntry("被旋转的四元数", Type.QUATERNION)
            .addEntry("旋转角度", Type.DOUBLE)
            .addDest(Type.QUATERNION, optional = true)
            .result("旋转四元数", Type.QUATERNION),
        Action.new("Math数学运算", "rotate四元数绕指定轴旋转", "quaternion", true)
            .description("四元数绕指定轴旋转")
            .addEntry("指定轴旋转标识符", Type.SYMBOL, head = "rotate")
            .addEntry("被旋转的四元数", Type.QUATERNION)
            .addEntry("旋转角度", Type.DOUBLE)
            .addEntry("转轴", Type.VECTOR)
            .result("旋转四元数", Type.QUATERNION),
        Action.new("Math数学运算", "通过插值获取两四元数中过渡态", "quaternion", true)
            .description("通过插值获取两四元数中过渡态")
            .addEntry("插值标识符", Type.SYMBOL, head = "nlerp")
            .addEntry("开始四元数", Type.QUATERNION)
            .addEntry("结束四元数", Type.QUATERNION)
            .addEntry("插值（0-1）", Type.DOUBLE)
            .addDest(Type.QUATERNION, optional = true)
            .result("旋转四元数", Type.QUATERNION),
        Action.new("Math数学运算", "通过插值获取两四元数中过渡态", "quaternion", true)
            .description("通过插值获取两四元数中过渡态")
            .addEntry("插值标识符", Type.SYMBOL, head = "slerp")
            .addEntry("开始四元数", Type.QUATERNION)
            .addEntry("结束四元数", Type.QUATERNION)
            .addEntry("插值（0-1）", Type.DOUBLE)
            .addDest(Type.QUATERNION, optional = true)
            .result("旋转四元数", Type.QUATERNION),
        Action.new("Math数学运算", "获取共轭的四元数", "quaternion", true)
            .description("获取共轭的四元数")
            .addEntry("共轭标识符", Type.SYMBOL, head = "conjugate")
            .addEntry("四元数", Type.QUATERNION)
            .addDest(Type.QUATERNION, optional = true)
            .result("旋转四元数", Type.QUATERNION),
        Action.new("Math数学运算", "应用四元数到指定向量", "quaternion", true)
            .description("应用四元数到指定向量")
            .addEntry("应用标识符", Type.SYMBOL, head = "transform")
            .addEntry("四元数", Type.QUATERNION)
            .addEntry("向量", Type.VECTOR)
            .addDest(Type.QUATERNION, optional = true)
            .result("应用后的向量", Type.VECTOR),
        Action.new("Math数学运算", "获得两向量之间的旋转四元数", "quaternion", true)
            .description("获得两向量之间的旋转四元数")
            .addEntry("标识符", Type.SYMBOL, head = "rotateTo")
            .addEntry("开始向量", Type.VECTOR)
            .addEntry("结束向量", Type.VECTOR)
            .addDest(Type.QUATERNION, optional = true)
            .result("应用后的四元数", Type.QUATERNION),
        Action.new("Math数学运算", "缩放四元数", "quaternion", true)
            .description("缩放四元数")
            .addEntry("缩放标识符", Type.SYMBOL, head = "scale")
            .addEntry("四元数", Type.QUATERNION)
            .addEntry("缩放值", Type.DOUBLE)
            .addDest(Type.QUATERNION, optional = true)
            .result("缩放后的四元数", Type.QUATERNION)
    ) {
        it.switch {
            case("create") { create(it) }
            case("identity") { actionNow { Quaterniond() } }
            case("rotateX") { rotateX(it) }
            case("rotateY") { rotateY(it) }
            case("rotateZ") { rotateZ(it) }
            case("rotate") { rotate(it) }
            case("nlerp") { nlerp(it) }
            case("slerp") { slerp(it) }
            case("conjugate") { conjugate(it) }
            case("transform") { transform(it) }
            case("rotateTo") { rotateTo(it) }
            case("scale") { scale(it) }
            other { actionNow { Quaterniond() } }
        }
    }

    private fun create(reader: QuestReader): ScriptAction<Any?> {

        val x = reader.nextParsedAction()
        val y = reader.nextParsedAction()
        val z = reader.nextParsedAction()
        val w = reader.nextParsedAction()

        return actionFuture { future ->
            run(x).double { x ->
                run(y).double { y ->
                    run(z).double { z ->
                        run(w).double { w ->
                            future.complete(Quaterniond(x, y, z, w))
                        }
                    }
                }
            }
        }
    }

    private fun rotateX(reader: QuestReader): ScriptAction<Any?> {

        val quaternion = reader.nextParsedAction()
        val angle = reader.nextParsedAction()
        val dest = reader.nextDest()

        return actionFuture { future ->
            run(quaternion).quaternion { quaternion ->
                run(angle).double { angle ->
                    destQuaternion(dest) {
                        future.complete(quaternion.rotateX(Math.toRadians(angle), it))
                    }
                }
            }
        }
    }

    private fun rotateY(reader: QuestReader): ScriptAction<Any?> {

        val quaternion = reader.nextParsedAction()
        val angle = reader.nextParsedAction()
        val dest = reader.nextDest()

        return actionFuture { future ->
            run(quaternion).quaternion { quaternion ->
                run(angle).double { angle ->
                    destQuaternion(dest) {
                        future.complete(quaternion.rotateY(Math.toRadians(angle), it))
                    }
                }
            }
        }
    }

    private fun rotateZ(reader: QuestReader): ScriptAction<Any?> {

        val quaternion = reader.nextParsedAction()
        val angle = reader.nextParsedAction()
        val dest = reader.nextDest()

        return actionFuture { future ->
            run(quaternion).quaternion { quaternion ->
                run(angle).double { angle ->
                    destQuaternion(dest) {
                        future.complete(quaternion.rotateZ(Math.toRadians(angle), it))
                    }
                }
            }
        }
    }

    private fun rotate(reader: QuestReader): ScriptAction<Any?> {

        val quaternion = reader.nextParsedAction()
        val angle = reader.nextParsedAction()
        val vector = reader.nextParsedAction()

        return actionFuture { future ->
            run(quaternion).quaternion { quaternion ->
                run(angle).double { angle ->
                    run(vector).vector { vector ->
                        future.complete(quaternion.rotateAxis(Math.toRadians(angle), vector))
                    }
                }
            }
        }
    }

    private fun nlerp(reader: QuestReader): ScriptAction<Any?> {

        val quaternion1 = reader.nextParsedAction()
        val quaternion2 = reader.nextParsedAction()
        val factor = reader.nextParsedAction()
        val dest = reader.nextDest()

        return actionFuture { future ->
            run(quaternion1).quaternion { quaternion1 ->
                run(quaternion2).quaternion { quaternion2 ->
                    run(factor).double { factor ->
                        destQuaternion(dest) {
                            future.complete(quaternion1.nlerp(quaternion2, factor, it))
                        }
                    }
                }
            }
        }
    }

    private fun slerp(reader: QuestReader): ScriptAction<Any?> {

        val quaternion1 = reader.nextParsedAction()
        val quaternion2 = reader.nextParsedAction()
        val factor = reader.nextParsedAction()
        val dest = reader.nextDest()

        return actionFuture { future ->
            run(quaternion1).quaternion { quaternion1 ->
                run(quaternion2).quaternion { quaternion2 ->
                    run(factor).double { factor ->
                        destQuaternion(dest) {
                            future.complete(quaternion1.slerp(quaternion2, factor, it))
                        }
                    }
                }
            }
        }
    }

    private fun conjugate(reader: QuestReader): ScriptAction<Any?> {

        val quaternion = reader.nextParsedAction()
        val dest = reader.nextDest()

        return actionFuture { future ->
            run(quaternion).quaternion { quaternion ->
                destQuaternion(dest) {
                    future.complete(quaternion.conjugate())
                }
            }
        }
    }

    private fun transform(reader: QuestReader): ScriptAction<Any?> {

        val quaternion = reader.nextParsedAction()
        val vector = reader.nextParsedAction()
        val dest = reader.nextDest()

        return actionFuture { future ->
            run(quaternion).quaternion { quaternion ->
                run(vector).vector { vector ->
                    destVector(dest) {
                        quaternion.transform(vector.joml, it.joml)
                        future.complete(it)
                    }
                }
            }
        }
    }

    private fun rotateTo(reader: QuestReader): ScriptAction<Any?> {

        val quaternion = reader.nextParsedAction()
        val vector1 = reader.nextParsedAction()
        val vector2 = reader.nextParsedAction()
        val dest = reader.nextDest()

        return actionFuture { future ->
            run(quaternion).quaternion { quaternion ->
                run(vector1).vector { vector1 ->
                    run(vector2).vector { vector2 ->
                        destQuaternion(dest) {
                            future.complete(quaternion.rotateTo(vector1, vector2, it))
                        }
                    }
                }
            }
        }
    }

    private fun scale(reader: QuestReader): ScriptAction<Any?> {

        val quaternion = reader.nextParsedAction()
        val factor = reader.nextParsedAction()
        val dest = reader.nextDest()

        return actionFuture { future ->
            run(quaternion).quaternion { quaternion ->
                run(factor).double { factor ->
                    destQuaternion(dest) {
                        future.complete(quaternion.scale(factor, it))
                    }
                }
            }
        }
    }
}