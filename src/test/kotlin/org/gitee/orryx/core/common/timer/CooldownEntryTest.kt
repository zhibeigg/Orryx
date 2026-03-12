package org.gitee.orryx.core.common.timer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CooldownEntryTest {

    @Test
    fun `new entry has correct tag`() {
        val entry = CooldownEntry("test", 1000)
        assertEquals("test", entry.tag)
    }

    @Test
    fun `new entry has correct remaining`() {
        val entry = CooldownEntry("test", 500)
        assertEquals(500, entry.remaining)
    }

    @Test
    fun `new entry is not ready when duration is positive`() {
        val entry = CooldownEntry("test", 10000)
        assertFalse(entry.isReady)
    }

    @Test
    fun `entry with zero duration is ready immediately`() {
        val entry = CooldownEntry("test", 0)
        assertTrue(entry.isReady)
    }

    @Test
    fun `countdown decreases over time`() {
        val entry = CooldownEntry("test", 500)
        val initial = entry.countdown
        Thread.sleep(50)
        val later = entry.countdown
        assertTrue(later < initial, "Countdown should decrease: initial=$initial, later=$later")
    }

    @Test
    fun `countdown never goes below zero`() {
        val entry = CooldownEntry("test", 10)
        Thread.sleep(50)
        assertTrue(entry.countdown >= 0)
    }

    @Test
    fun `entry becomes ready after duration expires`() {
        val entry = CooldownEntry("test", 30)
        Thread.sleep(60)
        assertTrue(entry.isReady)
    }

    @Test
    fun `addDuration increases remaining`() {
        val entry = CooldownEntry("test", 100)
        entry.addDuration(200)
        assertEquals(300, entry.remaining)
    }

    @Test
    fun `addDuration with negative clamps to zero`() {
        val entry = CooldownEntry("test", 100)
        entry.addDuration(-200)
        assertEquals(0, entry.remaining)
    }

    @Test
    fun `reduceDuration decreases remaining`() {
        val entry = CooldownEntry("test", 500)
        entry.reduceDuration(200)
        assertEquals(300, entry.remaining)
    }

    @Test
    fun `reduceDuration clamps to zero`() {
        val entry = CooldownEntry("test", 100)
        entry.reduceDuration(200)
        assertEquals(0, entry.remaining)
    }

    @Test
    fun `overStamp is timeStamp plus remaining`() {
        val before = System.currentTimeMillis()
        val entry = CooldownEntry("test", 1000)
        val after = System.currentTimeMillis()
        assertTrue(entry.overStamp in (before + 1000)..(after + 1000))
    }

    @Test
    fun `overStamp changes after addDuration`() {
        val entry = CooldownEntry("test", 1000)
        val original = entry.overStamp
        entry.addDuration(500)
        assertEquals(original + 500, entry.overStamp)
    }

    @Test
    fun `multiple addDuration calls accumulate`() {
        val entry = CooldownEntry("test", 100)
        entry.addDuration(50)
        entry.addDuration(50)
        assertEquals(200, entry.remaining)
    }

    @Test
    fun `multiple reduceDuration calls accumulate`() {
        val entry = CooldownEntry("test", 300)
        entry.reduceDuration(100)
        entry.reduceDuration(100)
        assertEquals(100, entry.remaining)
    }

    @Test
    fun `reduceDuration then addDuration works correctly`() {
        val entry = CooldownEntry("test", 500)
        entry.reduceDuration(300)
        entry.addDuration(100)
        assertEquals(300, entry.remaining)
    }
}
