package org.gitee.orryx

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.serialization.json.Json
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

object Orryx {

    private val skillCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, PlayerSkillPO>()

    @JvmStatic
    fun main(args: Array<String>) {
        var toJson = 0L
        var toSkill = 0L
        // 示例：测试反序列化耗时
        repeat(10000) {
            val skill = PlayerSkillPO(1, UUID.randomUUID(), "test", "test", false, 5)
            val json: String
            toJson += measureNanoTime {
                json = Json.encodeToString(skill)
            } / 1000 // 微秒级
            toSkill += measureNanoTime {
                Json.decodeFromString<PlayerSkillPO>(json)
            } / 1000 // 微秒级
        }
        println("toJson = ${toJson/10000}, toSkill = ${toSkill/10000}")
        // 示例：测试缓存耗时
        var test = 0L
        repeat(10000) {
            test += measureNanoTime {
                skillCache.get("test") {
                    PlayerSkillPO(1, UUID.randomUUID(), "test", "test", false, 5)
                }
            }
        }
        println("test = ${test/10000}")
        println("spirit".split(",").associateWith { it })
    }
}