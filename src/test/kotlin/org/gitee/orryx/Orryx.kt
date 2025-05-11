package org.gitee.orryx

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.serialization.json.Json
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import taboolib.module.kether.script
import java.util.*
import java.util.concurrent.TimeUnit

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
        for (i in 1..10000) {
            val skill = PlayerSkillPO(1, UUID.randomUUID(), "test", "test", false, 5)
            var start = System.nanoTime()
            val json = Json.encodeToString(skill)
            toJson += (System.nanoTime() - start) / 1000 // 微秒级
            start = System.nanoTime()
            Json.decodeFromString<PlayerSkillPO>(json)
            toSkill += (System.nanoTime() - start) / 1000 // 微秒级
        }
        println("toJson = ${toJson/10000}, toSkill = ${toSkill/10000}")
        // 示例：测试缓存耗时
        var test = 0L
        for (i in 1..10000) {
            val start = System.nanoTime()
            skillCache.get("test") {
                PlayerSkillPO(1, UUID.randomUUID(), "test", "test", false, 5)
            }
            test += (System.nanoTime() - start) // 纳秒级
        }
        println("test = ${test/10000}")
        println("spirit".split(",").associateWith { it })
    }
}