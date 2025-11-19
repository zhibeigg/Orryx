/**
 * 示例脚本 2: 计算器
 *
 * 演示脚本参数传递和计算功能
 */

// 获取参数
val operation = context["operation"] as? String ?: "add"
val num1 = context["num1"] as? Double ?: 0.0
val num2 = context["num2"] as? Double ?: 0.0

// 执行计算
val result = when (operation) {
    "add" -> num1 + num2
    "subtract" -> num1 - num2
    "multiply" -> num1 * num2
    "divide" -> if (num2 != 0.0) num1 / num2 else Double.NaN
    "power" -> Math.pow(num1, num2)
    "mod" -> num1 % num2
    else -> {
        player?.sendMessage("§c不支持的运算: $operation")
        Double.NaN
    }
}

// 显示结果
if (!result.isNaN()) {
    player?.sendMessage("§a计算结果: $num1 $operation $num2 = $result")
    context["result"] = result
} else {
    player?.sendMessage("§c计算失败")
}

// 返回计算结果
result
