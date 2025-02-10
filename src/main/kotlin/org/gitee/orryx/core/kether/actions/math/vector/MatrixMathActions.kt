package org.gitee.orryx.core.kether.actions.math.vector

import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import org.joml.Matrix4d
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object MatrixMathActions {


    /**
     * ```
     * set a matrix
     * set b entity direction they "@self"
     * matrix rotateX &a 30 dest a
     * matrix transform &a &b dest b
     * velocity &b they "@self"
     * ```
     * */
    @KetherParser(["matrix"], namespace = NAMESPACE, shared = true)
    private fun actionMatrix() = scriptParser(
        arrayOf(
            Action.new("Math数学运算", "创建单位矩阵", "matrix", true)
                .description("创建单位矩阵")
                .addEntry("创建单位矩阵标识符", Type.SYMBOL, true, "identity", "identity")
                .result("创建的单位矩阵", Type.MATRIX),
            Action.new("Math数学运算", "创建矩阵", "matrix", true)
                .description("创建矩阵")
                .addEntry("创建标识符", Type.SYMBOL, head = "create")
                .addEntry("0行0列", Type.DOUBLE)
                .addEntry("0行1列", Type.DOUBLE)
                .addEntry("0行2列", Type.DOUBLE)
                .addEntry("0行3列", Type.DOUBLE)
                .addEntry("1行0列", Type.DOUBLE)
                .addEntry("1行1列", Type.DOUBLE)
                .addEntry("1行2列", Type.DOUBLE)
                .addEntry("1行3列", Type.DOUBLE)
                .addEntry("2行0列", Type.DOUBLE)
                .addEntry("2行1列", Type.DOUBLE)
                .addEntry("2行2列", Type.DOUBLE)
                .addEntry("2行3列", Type.DOUBLE)
                .addEntry("3行0列", Type.DOUBLE)
                .addEntry("3行1列", Type.DOUBLE)
                .addEntry("3行2列", Type.DOUBLE)
                .addEntry("3行3列", Type.DOUBLE)
                .result("创建的矩阵", Type.MATRIX),
            Action.new("Math数学运算", "rotateX矩阵绕X轴旋转", "matrix", true)
                .description("矩阵绕X轴旋转")
                .addEntry("X轴旋转标识符", Type.SYMBOL, head = "rotateX")
                .addEntry("被旋转的矩阵", Type.MATRIX)
                .addEntry("旋转角度", Type.DOUBLE)
                .addDest(Type.MATRIX, optional = true)
                .result("旋转矩阵", Type.MATRIX),
            Action.new("Math数学运算", "rotateY矩阵绕Y轴旋转", "matrix", true)
                .description("矩阵绕Y轴旋")
                .addEntry("Y轴旋转标识符", Type.SYMBOL, head = "rotateY")
                .addEntry("被旋转的矩阵", Type.MATRIX)
                .addEntry("旋转角度", Type.DOUBLE)
                .addDest(Type.MATRIX, optional = true)
                .result("旋转矩阵", Type.MATRIX),
            Action.new("Math数学运算", "rotateZ矩阵绕Z轴旋转", "matrix", true)
                .description("矩阵绕Z轴旋转")
                .addEntry("Z轴旋转标识符", Type.SYMBOL, head = "rotateZ")
                .addEntry("被旋转的矩阵", Type.MATRIX)
                .addEntry("旋转角度", Type.DOUBLE)
                .addDest(Type.MATRIX, optional = true)
                .result("旋转矩阵", Type.MATRIX),
            Action.new("Math数学运算", "rotate矩阵绕向量轴旋转", "matrix", true)
                .description("矩阵绕给定轴旋转")
                .addEntry("给定轴旋转标识符", Type.SYMBOL, head = "rotate")
                .addEntry("被旋转的矩阵", Type.MATRIX)
                .addEntry("旋转的轴向量", Type.VECTOR)
                .addEntry("旋转角度", Type.DOUBLE)
                .addDest(Type.MATRIX, optional = true)
                .result("旋转矩阵", Type.MATRIX),
            Action.new("Math数学运算", "rotate矩阵统一缩放", "matrix", true)
                .description("矩阵按照系数统一缩放")
                .addEntry("统一缩放标识符", Type.SYMBOL, head = "scale")
                .addEntry("被缩放的矩阵", Type.MATRIX)
                .addEntry("缩放系数", Type.DOUBLE)
                .addDest(Type.MATRIX, optional = true)
                .result("缩放矩阵", Type.MATRIX),
            Action.new("Math数学运算", "rotate矩阵缩放", "matrix", true)
                .description("矩阵分别按照XYZ系数缩放")
                .addEntry("缩放标识符", Type.SYMBOL, head = "scaleXYZ")
                .addEntry("被缩放的矩阵", Type.MATRIX)
                .addEntry("缩放系数X", Type.DOUBLE)
                .addEntry("缩放系数Y", Type.DOUBLE)
                .addEntry("缩放系数Z", Type.DOUBLE)
                .addDest(Type.MATRIX, optional = true)
                .result("缩放矩阵", Type.MATRIX),
            Action.new("Math数学运算", "rotate矩阵位移", "matrix", true)
                .description("矩阵分别按照XYZ偏移")
                .addEntry("位移标识符", Type.SYMBOL, head = "translate")
                .addEntry("被缩放的矩阵", Type.MATRIX)
                .addEntry("偏移X", Type.DOUBLE)
                .addEntry("偏移Y", Type.DOUBLE)
                .addEntry("偏移Z", Type.DOUBLE)
                .addDest(Type.MATRIX, optional = true)
                .result("偏移矩阵", Type.MATRIX),
            Action.new("Math数学运算", "transform应用矩阵变换向量", "matrix", true)
                .description("应用矩阵变换向量")
                .addEntry("应用矩阵标识符", Type.SYMBOL, head = "transform")
                .addEntry("应用的矩阵", Type.MATRIX)
                .addEntry("向量", Type.VECTOR)
                .addDest(Type.VECTOR, optional = true)
                .result("变换后的向量", Type.VECTOR),
        )
    ) {
        it.switch {
            case("create") { create(it) }
            case("identity") { actionNow { Matrix4d() } }
            case("rotateX") { rotateX(it) }
            case("rotateY") { rotateY(it) }
            case("rotateZ") { rotateZ(it) }
            case("rotate") { rotate(it) }
            case("scale") { scale(it) }
            case("scaleXYZ") { scaleXYZ(it) }
            case("translate") { translate(it) }
            case("transform") { transform(it) }
            other { actionNow { Matrix4d() } }
        }
    }

    private fun create(reader: QuestReader): ScriptAction<Any?> {
        val m00 = reader.nextParsedAction()
        val m01 = reader.nextParsedAction()
        val m02 = reader.nextParsedAction()
        val m03 = reader.nextParsedAction()
        val m10 = reader.nextParsedAction()
        val m11 = reader.nextParsedAction()
        val m12 = reader.nextParsedAction()
        val m13 = reader.nextParsedAction()
        val m20 = reader.nextParsedAction()
        val m21 = reader.nextParsedAction()
        val m22 = reader.nextParsedAction()
        val m23 = reader.nextParsedAction()
        val m30 = reader.nextParsedAction()
        val m31 = reader.nextParsedAction()
        val m32 = reader.nextParsedAction()
        val m33 = reader.nextParsedAction()
        return actionFuture { future ->
            run(m00).double { m00 ->
                run(m01).double { m01 ->
                    run(m02).double { m02 ->
                        run(m03).double { m03 ->
                            run(m10).double { m10 ->
                                run(m11).double { m11 ->
                                    run(m12).double { m12 ->
                                        run(m13).double { m13 ->
                                            run(m20).double { m20 ->
                                                run(m21).double { m21 ->
                                                    run(m22).double { m22 ->
                                                        run(m23).double { m23 ->
                                                            run(m30).double { m30 ->
                                                                run(m31).double { m31 ->
                                                                    run(m32).double { m32 ->
                                                                        run(m33).double { m33 ->
                                                                            future.complete(Matrix4d(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33))
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun rotateX(reader: QuestReader): ScriptAction<Any?> {
        val matrix = reader.nextParsedAction()
        val angle = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(matrix).matrix { matrix ->
                run(angle).double { angle ->
                    destMatrix(dest) {
                        future.complete(matrix.rotateX(Math.toRadians(angle), it))
                    }
                }
            }
        }
    }

    private fun rotateY(reader: QuestReader): ScriptAction<Any?> {
        val matrix = reader.nextParsedAction()
        val angle = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(matrix).matrix { matrix ->
                run(angle).double { angle ->
                    destMatrix(dest) {
                        future.complete(matrix.rotateY(Math.toRadians(angle), it))
                    }
                }
            }
        }
    }

    private fun rotateZ(reader: QuestReader): ScriptAction<Any?> {
        val matrix = reader.nextParsedAction()
        val angle = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(matrix).matrix { matrix ->
                run(angle).double { angle ->
                    destMatrix(dest) {
                        future.complete(matrix.rotateZ(Math.toRadians(angle), it))
                    }
                }
            }
        }
    }

    private fun rotate(reader: QuestReader): ScriptAction<Any?> {
        val matrix = reader.nextParsedAction()
        val vector = reader.nextParsedAction()
        val angle = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(matrix).matrix { matrix ->
                run(vector).vector { vector ->
                    run(angle).double { angle ->
                        destMatrix(dest) {
                            future.complete(matrix.rotate(Math.toRadians(angle), vector.joml, it))
                        }
                    }
                }
            }
        }
    }

    private fun scale(reader: QuestReader): ScriptAction<Any?> {
        val matrix = reader.nextParsedAction()
        val scale = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(matrix).matrix { matrix ->
                run(scale).double { scale ->
                    destMatrix(dest) {
                        future.complete(matrix.scale(scale, it))
                    }
                }
            }
        }
    }

    private fun scaleXYZ(reader: QuestReader): ScriptAction<Any?> {
        val matrix = reader.nextParsedAction()
        val scaleX = reader.nextParsedAction()
        val scaleY = reader.nextParsedAction()
        val scaleZ = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(matrix).matrix { matrix ->
                run(scaleX).double { scaleX ->
                    run(scaleY).double { scaleY ->
                        run(scaleZ).double { scaleZ ->
                            destMatrix(dest) {
                                future.complete(matrix.scale(scaleX, scaleY, scaleZ, it))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun translate(reader: QuestReader): ScriptAction<Any?> {
        val matrix = reader.nextParsedAction()
        val offsetX = reader.nextParsedAction()
        val offsetY = reader.nextParsedAction()
        val offsetZ = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(matrix).matrix { matrix ->
                run(offsetX).double { offsetX ->
                    run(offsetY).double { offsetY ->
                        run(offsetZ).double { offsetZ ->
                            destMatrix(dest) {
                                future.complete(matrix.translate(offsetX, offsetY, offsetZ, it))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun transform(reader: QuestReader): ScriptAction<Any?> {
        val matrix = reader.nextParsedAction()
        val vector = reader.nextParsedAction()
        val dest = reader.nextDest()
        return actionFuture { future ->
            run(matrix).matrix { matrix ->
                run(vector).vector { vector ->
                    destVector(dest) {
                        future.complete(matrix.transformProject(vector, it.joml))
                    }
                }
            }
        }
    }

}