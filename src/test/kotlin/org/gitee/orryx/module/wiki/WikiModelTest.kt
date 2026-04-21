package org.gitee.orryx.module.wiki

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class WikiModelTest {

    @Nested
    inner class TypeEnumTest {
        @Test
        fun `all types exist`() {
            val expected = setOf(
                "ITERABLE", "CONTAINER", "HITBOX", "TARGET", "STATE", "EFFECT", "EFFECT_SPAWNER",
                "BYTE", "SHORT", "INT", "LONG", "DOUBLE", "FLOAT", "SYMBOL", "STRING", "BOOLEAN",
                "VECTOR", "MATRIX", "QUATERNION", "ITEM_STACK", "NBT", "PLAYER", "ANY", "NULL"
            )
            assertEquals(expected, Type.entries.map { it.name }.toSet())
        }

        @Test
        fun `count is 24`() {
            assertEquals(24, Type.entries.size)
        }
    }

    @Nested
    inner class SelectorTypeEnumTest {
        @Test
        fun `values`() {
            assertEquals(2, SelectorType.entries.size)
            assertNotNull(SelectorType.valueOf("STREAM"))
            assertNotNull(SelectorType.valueOf("GEOMETRY"))
        }
    }

    @Nested
    inner class TriggerGroupEnumTest {
        @Test
        fun `all groups exist`() {
            assertEquals(7, TriggerGroup.entries.size)
        }

        @Test
        fun `display values`() {
            assertEquals("Bukkit原版", TriggerGroup.BUKKIT.value)
            assertEquals("Orryx自身", TriggerGroup.ORRYX.value)
            assertEquals("DungeonPlus地牢", TriggerGroup.DUNGEON_PLUS.value)
            assertEquals("DragonCore龙核", TriggerGroup.DRAGONCORE.value)
            assertEquals("GermPlugin萌芽", TriggerGroup.GERM_PLUGIN.value)
            assertEquals("ArcartX挨插", TriggerGroup.ARCARTX.value)
            assertEquals("MythicMobs怪物", TriggerGroup.MYTHIC_MOBS.value)
        }
    }

    @Nested
    inner class ActionModelTest {
        @Test
        fun `create action with factory`() {
            val action = Action.new("TestGroup", "TestAction", "test", true)
            assertEquals("TestGroup", action.group)
            assertEquals("TestAction", action.name)
            assertEquals("test", action.key)
            assertTrue(action.sharded)
            assertTrue(action.entries.isEmpty())
        }

        @Test
        fun `add entries fluently`() {
            val action = Action.new("G", "A", "k", false)
                .addEntry("desc1", Type.INT, false, null, "head1")
                .addEntry("desc2", Type.STRING, true, "default", "head2")
            assertEquals(2, action.entries.size)
            assertEquals("desc1", action.entries[0].description)
            assertEquals(Type.INT, action.entries[0].type)
            assertFalse(action.entries[0].optional)
            assertNull(action.entries[0].default)
            assertEquals("head1", action.entries[0].head)
            assertTrue(action.entries[1].optional)
            assertEquals("default", action.entries[1].default)
        }

        @Test
        fun `add container entry`() {
            val action = Action.new("G", "A", "k", false)
                .addContainerEntry()
            assertEquals(1, action.entries.size)
            assertEquals(Type.CONTAINER, action.entries[0].type)
            assertEquals("they", action.entries[0].head)
        }

        @Test
        fun `add dest entry`() {
            val action = Action.new("G", "A", "k", false)
                .addDest(Type.STRING)
            assertEquals(1, action.entries.size)
            assertEquals("dest", action.entries[0].head)
        }

        @Test
        fun `add job entry`() {
            val action = Action.new("G", "A", "k", false)
                .addJob()
            assertEquals(1, action.entries.size)
            assertEquals("job", action.entries[0].head)
        }

        @Test
        fun `set description`() {
            val action = Action.new("G", "A", "k", false)
                .description("test desc")
            assertEquals("test desc", action.description)
        }

        @Test
        fun `set result`() {
            val action = Action.new("G", "A", "k", false)
                .result("result desc", Type.BOOLEAN)
            assertEquals(Type.BOOLEAN, action.result)
            assertEquals("result desc", action.resultDescription)
        }

        @Test
        fun `add example`() {
            val action = Action.new("G", "A", "k", false)
                .example("line1")
                .example("line2")
            assertEquals(2, action.example.size)
        }

        @Test
        fun `id format`() {
            val action = Action.new("G", "TestAction", "test", false)
            assertEquals("test TestAction", action.id())
        }

        @Test
        fun `default result is NULL`() {
            val action = Action.new("G", "A", "k", false)
            assertEquals(Type.NULL, action.result)
            assertNull(action.resultDescription)
        }
    }

    @Nested
    inner class TriggerModelTest {
        @Test
        fun `create trigger with factory`() {
            val trigger = Trigger.new(TriggerGroup.BUKKIT, "TestTrigger")
            assertEquals(TriggerGroup.BUKKIT, trigger.group)
            assertEquals("TestTrigger", trigger.key)
            assertEquals("", trigger.description)
        }

        @Test
        fun `add params fluently`() {
            val trigger = Trigger.new(TriggerGroup.ORRYX, "T")
                .addParm(Type.PLAYER, "player", "触发玩家")
                .addParm(Type.DOUBLE, "amount", "数量")
            assertEquals(2, trigger.entries.size)
            assertEquals(Type.PLAYER, trigger.entries[0].type)
            assertEquals("player", trigger.entries[0].key)
        }

        @Test
        fun `add special key`() {
            val trigger = Trigger.new(TriggerGroup.ORRYX, "T")
                .addSpecialKey(Type.STRING, "special", "特殊配置")
            assertEquals(1, trigger.specialKeyEntries.size)
        }

        @Test
        fun `set description`() {
            val trigger = Trigger.new(TriggerGroup.ORRYX, "T")
                .description("test desc")
            assertEquals("test desc", trigger.description)
        }
    }

    @Nested
    inner class SelectorModelTest {
        @Test
        fun `create selector with factory`() {
            val selector = Selector.new("Range", arrayOf("RANGE", "R"), SelectorType.GEOMETRY)
            assertEquals("Range", selector.name)
            assertArrayEquals(arrayOf("RANGE", "R"), selector.keys)
            assertEquals(SelectorType.GEOMETRY, selector.type)
        }

        @Test
        fun `add params fluently`() {
            val selector = Selector.new("Range", arrayOf("RANGE"), SelectorType.GEOMETRY)
                .addParm(Type.DOUBLE, "半径", "5")
            assertEquals(1, selector.entries.size)
            assertEquals(Type.DOUBLE, selector.entries[0].type)
            assertEquals("半径", selector.entries[0].description)
            assertEquals("5", selector.entries[0].default)
        }

        @Test
        fun `add example`() {
            val selector = Selector.new("Range", arrayOf("RANGE"), SelectorType.GEOMETRY)
                .addExample("@range 5")
            assertEquals(1, selector.example.size)
        }

        @Test
        fun `set description`() {
            val selector = Selector.new("Range", arrayOf("RANGE"), SelectorType.GEOMETRY)
                .description("范围选择器")
            assertEquals("范围选择器", selector.description)
        }
    }

    @Nested
    inner class PropertyModelTest {
        @Test
        fun `create property with factory`() {
            val prop = Property.new("Job", "IPlayerJob", "orryx.player.job")
            assertEquals("Job", prop.group)
            assertEquals("IPlayerJob", prop.name)
            assertEquals("orryx.player.job", prop.id)
        }

        @Test
        fun `add entries fluently`() {
            val prop = Property.new("Job", "IPlayerJob", "orryx.player.job")
                .addEntry("name", Type.STRING, "职业名称", false)
                .addEntry("level", Type.INT, "等级", true)
            assertEquals(2, prop.entries.size)
            assertEquals("name", prop.entries[0].key)
            assertFalse(prop.entries[0].writable)
            assertTrue(prop.entries[1].writable)
        }

        @Test
        fun `set description`() {
            val prop = Property.new("Job", "IPlayerJob", "orryx.player.job")
                .description("玩家职业属性")
            assertEquals("玩家职业属性", prop.description)
        }
    }
}
