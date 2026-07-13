package org.gitee.orryx.module.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.UUID

class OwnerViewerIndexTest {

    private data class Hud(val owner: UUID, val viewer: UUID, val name: String)

    @Test
    fun `registering a viewer replaces old owner entry`() {
        val index = OwnerViewerIndex<Hud>({ it.owner }, { it.viewer })
        val viewer = UUID.randomUUID()
        val first = Hud(UUID.randomUUID(), viewer, "first")
        val second = Hud(UUID.randomUUID(), viewer, "second")

        index.register(first)
        assertSame(first, index.register(second))

        assertSame(second, index.getViewer(viewer))
        assertNull(index.byOwner[first.owner])
        assertSame(second, index.byOwner[second.owner]?.get(viewer))
    }

    @Test
    fun `removing player clears both owner and viewer roles`() {
        val index = OwnerViewerIndex<Hud>({ it.owner }, { it.viewer })
        val player = UUID.randomUUID()
        val viewedByPlayer = Hud(UUID.randomUUID(), player, "viewer")
        val ownedForFirstViewer = Hud(player, UUID.randomUUID(), "owner-1")
        val ownedForSecondViewer = Hud(player, UUID.randomUUID(), "owner-2")
        listOf(viewedByPlayer, ownedForFirstViewer, ownedForSecondViewer).forEach(index::register)

        val removed = index.removePlayer(player)

        assertEquals(setOf(viewedByPlayer, ownedForFirstViewer, ownedForSecondViewer), removed)
        assertNull(index.getViewer(player))
        assertNull(index.byOwner[player])
        assertEquals(emptySet<Hud>(), index.values().toSet())
    }
}
