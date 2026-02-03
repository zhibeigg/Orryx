package org.gitee.orryx.core.kether.actions.math

import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.combinationParser
import org.gitee.orryx.utils.scriptParser
import taboolib.module.kether.KetherParser
import taboolib.module.kether.actionNow
import kotlin.math.*

object MathActions {

    @KetherParser(["sin"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionSin() = combinationParser(
        Action.new("Math数学运算", "求sin值", "sin", true)
            .description("求sin值")
            .addEntry("角度值", Type.DOUBLE, false)
            .result("sin值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { degrees ->
            now {
                sin(Math.toRadians(degrees))
            }
        }
    }

    @KetherParser(["cos"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionCos() = combinationParser(
        Action.new("Math数学运算", "求cos值", "cos", true)
            .description("求cos值")
            .addEntry("角度值", Type.DOUBLE, false)
            .result("cos值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { degrees ->
            now {
                cos(Math.toRadians(degrees))
            }
        }
    }

    @KetherParser(["tan"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionTan() = combinationParser(
        Action.new("Math数学运算", "求tan值", "tan", true)
            .description("求tan值")
            .addEntry("角度值", Type.DOUBLE, false)
            .result("tan值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { degrees ->
            now {
                tan(Math.toRadians(degrees))
            }
        }
    }

    @KetherParser(["asin"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionAsin() = combinationParser(
        Action.new("Math数学运算", "求asin值", "asin", true)
            .description("求asin值")
            .addEntry("值", Type.DOUBLE, false)
            .result("角度值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { sin ->
            now {
                Math.toDegrees(asin(sin))
            }
        }
    }

    @KetherParser(["acos"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionAcos() = combinationParser(
        Action.new("Math数学运算", "求acos值", "acos", true)
            .description("求acos值")
            .addEntry("值", Type.DOUBLE, false)
            .result("角度值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { cos ->
            now {
                Math.toDegrees(acos(cos))
            }
        }
    }

    @KetherParser(["atan"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionAtan() = combinationParser(
        Action.new("Math数学运算", "求atan值", "atan", true)
            .description("求atan值")
            .addEntry("值", Type.DOUBLE, false)
            .result("角度值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { tan ->
            now {
                Math.toDegrees(atan(tan))
            }
        }
    }

    @KetherParser(["degrees"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionToDegrees() = combinationParser(
        Action.new("Math数学运算", "将弧度值转化为角度值", "degrees", true)
            .description("将弧度值转化为角度值")
            .addEntry("弧度值", Type.DOUBLE, false)
            .result("角度值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { radians ->
            now {
                Math.toDegrees(radians)
            }
        }
    }

    @KetherParser(["radians"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionToRadians() = combinationParser(
        Action.new("Math数学运算", "将角度值转化为弧度值", "radians", true)
            .description("将角度值转化为弧度值")
            .addEntry("角度值", Type.DOUBLE, false)
            .result("弧度值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { degrees ->
            now {
                Math.toRadians(degrees)
            }
        }
    }

    @KetherParser(["abs"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionABS() = combinationParser(
        Action.new("Math数学运算", "取绝对值", "abs", true)
            .description("取绝对值")
            .addEntry("值", Type.DOUBLE, false)
            .result("绝对值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { number ->
            now {
                abs(number)
            }
        }
    }

    @KetherParser(["floor"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionFloor() = combinationParser(
        Action.new("Math数学运算", "向下取整", "floor", true)
            .description("向下取整")
            .addEntry("值", Type.DOUBLE, false)
            .result("值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { number ->
            now {
                floor(number)
            }
        }
    }

    @KetherParser(["ceil"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionCeil() = combinationParser(
        Action.new("Math数学运算", "向上取整", "ceil", true)
            .description("向上取整")
            .addEntry("值", Type.DOUBLE, false)
            .result("值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { number ->
            now {
                ceil(number)
            }
        }
    }

    @KetherParser(["truncate"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionTruncate() = combinationParser(
        Action.new("Math数学运算", "向0取整", "truncate", true)
            .description("向0取整")
            .addEntry("值", Type.DOUBLE, false)
            .result("值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { number ->
            now {
                truncate(number)
            }
        }
    }

    @KetherParser(["sqrt"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionSqrt() = combinationParser(
        Action.new("Math数学运算", "计算平方", "sqrt", true)
            .description("计算平方")
            .addEntry("值", Type.DOUBLE, false)
            .result("平方值", Type.DOUBLE)
    ) {
        it.group(
            double()
        ).apply(it) { number ->
            now {
                sqrt(number)
            }
        }
    }

    @KetherParser(["pow"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionPow() = combinationParser(
        Action.new("Math数学运算", "计算幂函数", "pow", true)
            .description("计算幂函数")
            .addEntry("底", Type.DOUBLE, false)
            .addEntry("幂", Type.DOUBLE, false)
            .result("幂函数值", Type.DOUBLE)
    ) {
        it.group(
            double(),
            double()
        ).apply(it) { number, power ->
            now {
                number.pow(power)
            }
        }
    }

    @KetherParser(["natural"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionNatural() = scriptParser(
        Action.new("Math数学运算", "自然对数函数的底数", "natural", true)
            .description("自然对数函数的底数")
            .result("自然对数函数的底数", Type.DOUBLE)
    ) {
        actionNow { E }
    }

    @KetherParser(["pi"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionPI() = scriptParser(
        Action.new("Math数学运算", "圆周率", "pi", true)
            .description("圆周率")
            .result("圆周率", Type.DOUBLE)
    ) {
        actionNow { PI }
    }
}