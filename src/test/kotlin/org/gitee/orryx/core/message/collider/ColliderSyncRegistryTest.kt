package org.gitee.orryx.core.message.collider

import io.mockk.every
import io.mockk.mockk
import org.bukkit.World
import org.bukkit.entity.Player
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ColliderSyncRegistryTest {

    @Test
    fun `静止不重发且移动后发送更新`() {
        val fixture = RegistryFixture()
        val sphere = MockSphere(Vector3d(), 1.0)
        assertTrue(
            fixture.registry.show(
                fixture.player,
                "moving",
                sphere,
                ColliderRenderColor.WHITE,
                realtime = true,
                intervalTicks = 1L,
                settings = fixture.settings,
            )
        )

        fixture.registry.tick(fixture.settings)
        assertEquals(0, fixture.sender.updates.size)

        sphere.center.x = 2.0
        fixture.registry.tick(fixture.settings)
        assertEquals(1, fixture.sender.updates.size)
        assertEquals(2.0, (fixture.sender.updates.single().shape as ColliderWireShape.Sphere).centerX)

        fixture.registry.tick(fixture.settings)
        assertEquals(1, fixture.sender.updates.size)
    }

    @Test
    fun `发包预算公平顺延且禁用后移除`() {
        val fixture = RegistryFixture()
        val first = MockSphere(Vector3d(), 1.0)
        val second = MockSphere(Vector3d(), 1.0)
        val settings = fixture.settings.copy(maxPacketsPerTick = 1)
        fixture.registry.show(fixture.player, "first", first, ColliderRenderColor.WHITE, true, 1L, settings)
        fixture.registry.show(fixture.player, "second", second, ColliderRenderColor.WHITE, true, 1L, settings)
        first.center.x = 1.0
        second.center.x = 2.0

        fixture.registry.tick(settings)
        assertEquals(1, fixture.sender.updates.size)
        fixture.registry.tick(settings)
        assertEquals(2, fixture.sender.updates.size)
        assertEquals(setOf("first", "second"), fixture.sender.updates.map { it.id }.toSet())

        first.setDisable(true)
        fixture.registry.tick(settings.copy(maxPacketsPerTick = 2))
        assertFalse(fixture.registry.contains(fixture.playerId, "first"))
        assertTrue(fixture.sender.removes.contains("first"))
    }

    @Test
    fun `容量淘汰最旧条目且世界变化会清理`() {
        val fixture = RegistryFixture()
        val settings = fixture.settings.copy(maxTrackedPerViewer = 1)
        fixture.registry.show(
            fixture.player,
            "old",
            MockSphere(Vector3d(), 1.0),
            ColliderRenderColor.WHITE,
            realtime = false,
            intervalTicks = 1L,
            settings = settings,
        )
        fixture.registry.show(
            fixture.player,
            "new",
            MockSphere(Vector3d(), 1.0),
            ColliderRenderColor.WHITE,
            realtime = true,
            intervalTicks = 1L,
            settings = settings,
        )
        assertFalse(fixture.registry.contains(fixture.playerId, "old"))
        assertTrue(fixture.registry.contains(fixture.playerId, "new"))
        assertTrue(fixture.sender.removes.contains("old"))

        fixture.worldId = UUID.randomUUID()
        fixture.registry.tick(settings)
        assertEquals(0, fixture.registry.size())
        assertTrue(fixture.sender.removes.contains("new"))
    }

    private class RegistryFixture {
        val playerId: UUID = UUID.randomUUID()
        var worldId: UUID = UUID.randomUUID()
        private val world = mockk<World>()
        val player = mockk<Player>()
        val sender = RecordingSender()
        val settings = ColliderSyncSettings(
            defaultIntervalTicks = 1L,
            maxTrackedPerViewer = 200,
            maxChecksPerTick = 100,
            maxPacketsPerTick = 100,
        )
        val registry: ColliderSyncRegistry

        init {
            every { world.uid } answers { worldId }
            every { player.uniqueId } returns playerId
            every { player.isOnline } returns true
            every { player.world } returns world
            registry = ColliderSyncRegistry(
                playerResolver = { id -> if (id == playerId) player else null },
                sender = sender,
            )
        }
    }

    private class RecordingSender : ColliderPacketSender {
        val shows = mutableListOf<ColliderWireSnapshot>()
        val updates = mutableListOf<ColliderWireSnapshot>()
        val removes = mutableListOf<String>()

        override fun show(viewer: Player, snapshot: ColliderWireSnapshot): Boolean {
            shows += snapshot
            return true
        }

        override fun update(viewer: Player, snapshot: ColliderWireSnapshot): Boolean {
            updates += snapshot
            return true
        }

        override fun remove(viewer: Player, id: String): Boolean {
            removes += id
            return true
        }
    }
}
