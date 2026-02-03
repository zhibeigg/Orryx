package org.gitee.orryx.core.common

import org.jetbrains.annotations.NotNull
import java.security.SecureRandom
import java.util.Random
import kotlin.math.abs
import kotlin.math.ceil

/**
 * NanoId 是一个用于生成安全、URL友好的唯一标识符的工具对象。
 *
 * 该对象提供了生成随机字符串的方法，支持调整参数如长度、字母表、
 * 额外字节因子以及自定义随机数生成器。
 *
 * 使用示例：
 * ```
 * val id = NanoId.generate()
 * ```
 */
object NanoId {

    /**
     * 根据指定或默认参数生成随机字符串。
     *
     * @param size 生成字符串的期望长度。默认为 21。
     * @param alphabet 用于生成字符串的字符集。默认包含字母数字字符以及 "_" 和 "-"。
     * @param additionalBytesFactor 用于计算步长的额外字节因子。默认为 1.6。
     * @param random 使用的随机数生成器。默认为 `SecureRandom`。
     * @return 生成的随机字符串。
     * @throws IllegalArgumentException 如果字母表为空或超过 255 个字符，或者长度不大于零。
     */
    @JvmOverloads
    @JvmStatic
    fun generate(
        @NotNull
        size: Int = 21,
        @NotNull
        alphabet: String = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
        @NotNull
        additionalBytesFactor: Double = 1.6,
        @NotNull
        random: Random = SecureRandom()
    ): String {
        require(!(alphabet.isEmpty() || alphabet.length >= 256)) { "alphabet must contain between 1 and 255 symbols." }
        require(size > 0) { "size must be greater than zero." }
        require(additionalBytesFactor >= 1) { "additionalBytesFactor must be greater or equal 1." }

        val mask = calculateMask(alphabet)
        val step = calculateStep(size, alphabet, additionalBytesFactor)

        return generateOptimized(size, alphabet, mask, step, random)
    }

    /**
     * 使用给定的字母表、掩码和步长生成指定长度的优化随机字符串。
     * 可选地，你可以指定自定义随机数生成器。此优化版本专为更高性能和更低内存开销而设计。
     *
     * @param size 生成字符串的期望长度。
     * @param alphabet 用于生成字符串的字符集。
     * @param mask 用于将随机字节映射到字母表索引的掩码。应为 `(2^n) - 1`，其中 `n` 是小于或等于字母表大小的 2 的幂。
     * @param step 每次迭代生成的随机字节数。较大的值可能加快函数速度但会增加内存使用。
     * @param random 随机数生成器。默认为 `SecureRandom`。
     * @return 生成的优化字符串。
     */
    @JvmOverloads
    @JvmStatic
    fun generateOptimized(@NotNull size: Int, @NotNull alphabet: String, @NotNull mask: Int, @NotNull step: Int, @NotNull random: Random = SecureRandom()): String {
        val idBuilder = StringBuilder(size)
        val bytes = ByteArray(step)
        while (true) {
            random.nextBytes(bytes)
            for (i in 0 until step) {
                val alphabetIndex = bytes[i].toInt() and mask
                if (alphabetIndex < alphabet.length) {
                    idBuilder.append(alphabet[alphabetIndex])
                    if (idBuilder.length == size) {
                        return idBuilder.toString()
                    }
                }
            }
        }
    }

    /**
     * 计算生成步长所需的最佳额外字节因子，步长用于在每次迭代中生成随机字节。
     *
     * @param alphabet 用于生成字符串的字符集。
     * @return 额外字节因子，四舍五入到两位小数。
     */
    @JvmStatic
    fun calculateAdditionalBytesFactor(@NotNull alphabet: String): Double {
        val mask = calculateMask(alphabet)
        return (1 + abs((mask - alphabet.length.toDouble()) / alphabet.length)).round(2)
    }

    /**
     * 计算用于将随机字节映射到字母表索引的掩码。
     *
     * @param alphabet 用于生成字符串的字符集。
     * @return 计算出的掩码值。
     */
    @JvmStatic
    fun calculateMask(@NotNull alphabet: String) = (2 shl (Integer.SIZE - 1 - Integer.numberOfLeadingZeros(alphabet.length - 1))) - 1

    /**
     * 计算给定长度和字母表在每次迭代中生成的随机字节数。
     *
     * @param size 生成字符串的长度。
     * @param alphabet 用于生成字符串的字符集。
     * @param additionalBytesFactor 额外字节因子。默认值使用 `calculateAdditionalBytesFactor()` 计算。
     * @return 每次迭代生成的随机字节数。
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStep(@NotNull size: Int, @NotNull alphabet: String, @NotNull additionalBytesFactor: Double = calculateAdditionalBytesFactor(alphabet)) =
        ceil(additionalBytesFactor * calculateMask(alphabet) * size / alphabet.length).toInt()

    @JvmSynthetic
    internal fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}