package org.gitee.orryx.utils

import kotlinx.serialization.Serializable

/**
 * 可序列化的二元组类，用于存储两个相关联的值。
 *
 * 此类是为了避免 Kotlin 重定向导致其他插件依赖时无法使用标准库 [kotlin.Pair] 而创建的替代实现。
 * 命名为 [Tuple2] 以区别于 Kotlin 标准库的 [kotlin.Pair]。
 *
 * @param A 第一个元素的类型
 * @param B 第二个元素的类型
 * @property first 第一个元素
 * @property second 第二个元素
 *
 * @see Tuple3 三元组
 * @see paired 中缀函数，用于创建 [Tuple2] 实例
 */
@Serializable
data class Tuple2<out A, out B>(
    val first: A,
    val second: B
) {

    /**
     * 返回元组的字符串表示形式。
     *
     * @return 格式为 "(first, second)" 的字符串
     */
    override fun toString(): String = "($first, $second)"
}

/**
 * 创建一个 [Tuple2] 实例的中缀函数。
 *
 * 此函数命名为 [paired] 以区别于 Kotlin 标准库的 [kotlin.to] 扩展函数。
 *
 * 用法示例：
 * ```kotlin
 * val tuple = "key" paired 123
 * ```
 *
 * @param A 第一个元素的类型
 * @param B 第二个元素的类型
 * @param that 第二个元素
 * @return 包含接收者和 [that] 的 [Tuple2] 实例
 */
infix fun <A, B> A.paired(that: B): Tuple2<A, B> = Tuple2(this, that)

/**
 * 将具有相同类型元素的 [Tuple2] 转换为列表。
 *
 * @param T 元素类型
 * @return 包含 [Tuple2.first] 和 [Tuple2.second] 的列表
 */
fun <T> Tuple2<T, T>.toList(): List<T> = listOf(first, second)

/**
 * 可序列化的三元组类，用于存储三个相关联的值。
 *
 * 此类是为了避免 Kotlin 重定向导致其他插件依赖时无法使用标准库 [kotlin.Triple] 而创建的替代实现。
 * 命名为 [Tuple3] 以区别于 Kotlin 标准库的 [kotlin.Triple]。
 *
 * @param A 第一个元素的类型
 * @param B 第二个元素的类型
 * @param C 第三个元素的类型
 * @property first 第一个元素
 * @property second 第二个元素
 * @property third 第三个元素
 *
 * @see Tuple2 二元组
 */
@Serializable
data class Tuple3<out A, out B, out C>(
    val first: A,
    val second: B,
    val third: C
) {

    /**
     * 返回元组的字符串表示形式。
     *
     * @return 格式为 "(first, second, third)" 的字符串
     */
    override fun toString(): String = "($first, $second, $third)"
}

/**
 * 将具有相同类型元素的 [Tuple3] 转换为列表。
 *
 * @param T 元素类型
 * @return 包含 [Tuple3.first]、[Tuple3.second] 和 [Tuple3.third] 的列表
 */
fun <T> Tuple3<T, T, T>.toList(): List<T> = listOf(first, second, third)
