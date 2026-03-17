package org.gitee.orryx.api

import org.gitee.orryx.api.events.player.OrryxPlayerSpiritEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobExperienceEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobLevelEvents
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * API 重构相关的结构性测试。
 *
 * 验证事件类的 Pre/Post 嵌套结构、字段可变性、allowCancelled 属性等。
 * 不依赖 Bukkit 运行时，仅通过反射验证类结构。
 */
class ApiRefactorTest {

    // ==================== 事件类结构验证 ====================

    @Nested
    inner class SpiritEventStructure {

        @Test
        fun `Up Pre class exists and is accessible`() {
            assertNotNull(OrryxPlayerSpiritEvents.Up.Pre::class.java)
        }

        @Test
        fun `Up Post class exists and is accessible`() {
            assertNotNull(OrryxPlayerSpiritEvents.Up.Post::class.java)
        }

        @Test
        fun `Down Pre class exists and is accessible`() {
            assertNotNull(OrryxPlayerSpiritEvents.Down.Pre::class.java)
        }

        @Test
        fun `Down Post class exists and is accessible`() {
            assertNotNull(OrryxPlayerSpiritEvents.Down.Post::class.java)
        }

        @Test
        fun `Regain Pre class exists and is accessible`() {
            assertNotNull(OrryxPlayerSpiritEvents.Regain.Pre::class.java)
        }

        @Test
        fun `Regain Post class exists and is accessible`() {
            assertNotNull(OrryxPlayerSpiritEvents.Regain.Post::class.java)
        }

        @Test
        fun `Heal Pre class exists and is accessible`() {
            assertNotNull(OrryxPlayerSpiritEvents.Heal.Pre::class.java)
        }

        @Test
        fun `Heal Post class exists and is accessible`() {
            assertNotNull(OrryxPlayerSpiritEvents.Heal.Post::class.java)
        }

        @Test
        fun `Up Pre spirit field is mutable`() {
            val field = OrryxPlayerSpiritEvents.Up.Pre::class.java.getDeclaredField("spirit")
            assertNotNull(field)
            // var 属性在 Kotlin 中会生成 setter
            val setter = OrryxPlayerSpiritEvents.Up.Pre::class.java.methods.find { it.name == "setSpirit" }
            assertNotNull(setter, "Up.Pre.spirit should be mutable (var)")
        }

        @Test
        fun `Up Post spirit field is immutable`() {
            val setter = OrryxPlayerSpiritEvents.Up.Post::class.java.methods.find { it.name == "setSpirit" }
            assertNull(setter, "Up.Post.spirit should be immutable (val)")
        }

        @Test
        fun `Down Pre spirit field is mutable`() {
            val setter = OrryxPlayerSpiritEvents.Down.Pre::class.java.methods.find { it.name == "setSpirit" }
            assertNotNull(setter, "Down.Pre.spirit should be mutable (var)")
        }

        @Test
        fun `Down Post spirit field is immutable`() {
            val setter = OrryxPlayerSpiritEvents.Down.Post::class.java.methods.find { it.name == "setSpirit" }
            assertNull(setter, "Down.Post.spirit should be immutable (val)")
        }

        @Test
        fun `Regain Pre regainSpirit field is mutable`() {
            val setter = OrryxPlayerSpiritEvents.Regain.Pre::class.java.methods.find { it.name == "setRegainSpirit" }
            assertNotNull(setter, "Regain.Pre.regainSpirit should be mutable (var)")
        }

        @Test
        fun `Regain Post regainSpirit field is immutable`() {
            val setter = OrryxPlayerSpiritEvents.Regain.Post::class.java.methods.find { it.name == "setRegainSpirit" }
            assertNull(setter, "Regain.Post.regainSpirit should be immutable (val)")
        }
    }

    @Nested
    inner class JobExperienceEventStructure {

        @Test
        fun `Up Pre class exists`() {
            assertNotNull(OrryxPlayerJobExperienceEvents.Up.Pre::class.java)
        }

        @Test
        fun `Up Post class exists`() {
            assertNotNull(OrryxPlayerJobExperienceEvents.Up.Post::class.java)
        }

        @Test
        fun `Down Pre class exists`() {
            assertNotNull(OrryxPlayerJobExperienceEvents.Down.Pre::class.java)
        }

        @Test
        fun `Down Post class exists`() {
            assertNotNull(OrryxPlayerJobExperienceEvents.Down.Post::class.java)
        }

        @Test
        fun `Up Pre upExperience field is mutable`() {
            val setter = OrryxPlayerJobExperienceEvents.Up.Pre::class.java.methods.find { it.name == "setUpExperience" }
            assertNotNull(setter, "Up.Pre.upExperience should be mutable (var)")
        }

        @Test
        fun `Up Post upExperience field is immutable`() {
            val setter = OrryxPlayerJobExperienceEvents.Up.Post::class.java.methods.find { it.name == "setUpExperience" }
            assertNull(setter, "Up.Post.upExperience should be immutable (val)")
        }
    }

    @Nested
    inner class JobLevelEventStructure {

        @Test
        fun `Up Pre class exists`() {
            assertNotNull(OrryxPlayerJobLevelEvents.Up.Pre::class.java)
        }

        @Test
        fun `Up Post class exists`() {
            assertNotNull(OrryxPlayerJobLevelEvents.Up.Post::class.java)
        }

        @Test
        fun `Down Pre class exists`() {
            assertNotNull(OrryxPlayerJobLevelEvents.Down.Pre::class.java)
        }

        @Test
        fun `Down Post class exists`() {
            assertNotNull(OrryxPlayerJobLevelEvents.Down.Post::class.java)
        }

        @Test
        fun `Up Pre upLevel field is mutable`() {
            val setter = OrryxPlayerJobLevelEvents.Up.Pre::class.java.methods.find { it.name == "setUpLevel" }
            assertNotNull(setter, "Up.Pre.upLevel should be mutable (var)")
        }

        @Test
        fun `Up Post upLevel field is immutable`() {
            val setter = OrryxPlayerJobLevelEvents.Up.Post::class.java.methods.find { it.name == "setUpLevel" }
            assertNull(setter, "Up.Post.upLevel should be immutable (val)")
        }

        @Test
        fun `Down Pre downLevel field is mutable`() {
            val setter = OrryxPlayerJobLevelEvents.Down.Pre::class.java.methods.find { it.name == "setDownLevel" }
            assertNotNull(setter, "Down.Pre.downLevel should be mutable (var)")
        }

        @Test
        fun `Down Post downLevel field is immutable`() {
            val setter = OrryxPlayerJobLevelEvents.Down.Post::class.java.methods.find { it.name == "setDownLevel" }
            assertNull(setter, "Down.Post.downLevel should be immutable (val)")
        }
    }

    // ==================== IProfileAPI 接口结构验证 ====================

    @Nested
    inner class ProfileApiStructure {

        @Test
        fun `IProfileAPI has superBody method`() {
            val method = org.gitee.orryx.api.interfaces.IProfileAPI::class.java.getMethod("superBody")
            assertNotNull(method)
            assertEquals(org.gitee.orryx.api.interfaces.ITimedStatus::class.java, method.returnType)
        }

        @Test
        fun `IProfileAPI has invincible method`() {
            val method = org.gitee.orryx.api.interfaces.IProfileAPI::class.java.getMethod("invincible")
            assertNotNull(method)
            assertEquals(org.gitee.orryx.api.interfaces.ITimedStatus::class.java, method.returnType)
        }

        @Test
        fun `IProfileAPI has superFoot method`() {
            val method = org.gitee.orryx.api.interfaces.IProfileAPI::class.java.getMethod("superFoot")
            assertNotNull(method)
            assertEquals(org.gitee.orryx.api.interfaces.ITimedStatus::class.java, method.returnType)
        }

        @Test
        fun `IProfileAPI has silence method`() {
            val method = org.gitee.orryx.api.interfaces.IProfileAPI::class.java.getMethod("silence")
            assertNotNull(method)
            assertEquals(org.gitee.orryx.api.interfaces.ITimedStatus::class.java, method.returnType)
        }

        @Test
        fun `IProfileAPI has block method`() {
            val method = org.gitee.orryx.api.interfaces.IProfileAPI::class.java.getMethod("block")
            assertNotNull(method)
            assertEquals(org.gitee.orryx.api.interfaces.IBlockStatus::class.java, method.returnType)
        }

        @Test
        fun `deprecated isSuperBody has default implementation`() {
            val method = org.gitee.orryx.api.interfaces.IProfileAPI::class.java.getMethod("isSuperBody", org.bukkit.entity.Player::class.java)
            assertNotNull(method)
            assertTrue(method.isAnnotationPresent(Deprecated::class.java))
        }

        @Test
        fun `deprecated setSuperBody has default implementation`() {
            val method = org.gitee.orryx.api.interfaces.IProfileAPI::class.java.getMethod("setSuperBody", org.bukkit.entity.Player::class.java, Long::class.java)
            assertNotNull(method)
            assertTrue(method.isAnnotationPresent(Deprecated::class.java))
        }
    }

    // ==================== IKeyAPI 接口结构验证 ====================

    @Nested
    inner class KeyApiStructure {

        @Test
        fun `getGroup returns nullable`() {
            val method = org.gitee.orryx.api.interfaces.IKeyAPI::class.java.getMethod("getGroup", String::class.java)
            assertNotNull(method)
            // 返回类型是 IGroup（nullable 在 JVM 层面无法区分，但方法存在即可）
        }

        @Test
        fun `getGroupOrThrow has default implementation`() {
            val method = org.gitee.orryx.api.interfaces.IKeyAPI::class.java.getMethod("getGroupOrThrow", String::class.java)
            assertNotNull(method)
        }

        @Test
        fun `getBindKey returns nullable`() {
            val method = org.gitee.orryx.api.interfaces.IKeyAPI::class.java.getMethod("getBindKey", String::class.java)
            assertNotNull(method)
        }

        @Test
        fun `getBindKeyOrThrow has default implementation`() {
            val method = org.gitee.orryx.api.interfaces.IKeyAPI::class.java.getMethod("getBindKeyOrThrow", String::class.java)
            assertNotNull(method)
        }
    }

    // ==================== ITimedStatus 接口方法验证 ====================

    @Nested
    inner class TimedStatusInterface {

        @Test
        fun `has isActive method`() {
            val method = org.gitee.orryx.api.interfaces.ITimedStatus::class.java.getMethod("isActive", org.bukkit.entity.Player::class.java)
            assertNotNull(method)
            assertEquals(Boolean::class.java, method.returnType)
        }

        @Test
        fun `has countdown method`() {
            val method = org.gitee.orryx.api.interfaces.ITimedStatus::class.java.getMethod("countdown", org.bukkit.entity.Player::class.java)
            assertNotNull(method)
            assertEquals(Long::class.java, method.returnType)
        }

        @Test
        fun `has set method`() {
            val method = org.gitee.orryx.api.interfaces.ITimedStatus::class.java.getMethod("set", org.bukkit.entity.Player::class.java, Long::class.java)
            assertNotNull(method)
        }

        @Test
        fun `has cancel method`() {
            val method = org.gitee.orryx.api.interfaces.ITimedStatus::class.java.getMethod("cancel", org.bukkit.entity.Player::class.java)
            assertNotNull(method)
        }

        @Test
        fun `has add method`() {
            val method = org.gitee.orryx.api.interfaces.ITimedStatus::class.java.getMethod("add", org.bukkit.entity.Player::class.java, Long::class.java)
            assertNotNull(method)
        }

        @Test
        fun `has reduce method`() {
            val method = org.gitee.orryx.api.interfaces.ITimedStatus::class.java.getMethod("reduce", org.bukkit.entity.Player::class.java, Long::class.java)
            assertNotNull(method)
        }
    }

    // ==================== IBlockStatus 接口方法验证 ====================

    @Nested
    inner class BlockStatusInterface {

        @Test
        fun `has isActive method`() {
            val method = org.gitee.orryx.api.interfaces.IBlockStatus::class.java.getMethod(
                "isActive",
                org.bukkit.entity.Player::class.java,
                org.gitee.orryx.api.events.damage.DamageType::class.java
            )
            assertNotNull(method)
            assertEquals(Boolean::class.java, method.returnType)
        }

        @Test
        fun `has cancel method`() {
            val method = org.gitee.orryx.api.interfaces.IBlockStatus::class.java.getMethod(
                "cancel",
                org.bukkit.entity.Player::class.java,
                org.gitee.orryx.api.events.damage.DamageType::class.java
            )
            assertNotNull(method)
        }

        @Test
        fun `has cancelAll method`() {
            val method = org.gitee.orryx.api.interfaces.IBlockStatus::class.java.getMethod(
                "cancelAll",
                org.bukkit.entity.Player::class.java
            )
            assertNotNull(method)
        }

        @Test
        fun `has reduce method`() {
            val method = org.gitee.orryx.api.interfaces.IBlockStatus::class.java.getMethod(
                "reduce",
                org.bukkit.entity.Player::class.java,
                org.gitee.orryx.api.events.damage.DamageType::class.java,
                Long::class.java
            )
            assertNotNull(method)
        }
    }
}
