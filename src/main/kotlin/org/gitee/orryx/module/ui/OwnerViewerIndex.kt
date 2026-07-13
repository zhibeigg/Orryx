package org.gitee.orryx.module.ui

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class OwnerViewerIndex<T>(
    private val ownerId: (T) -> UUID,
    private val viewerId: (T) -> UUID
) {
    val byOwner = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, T>>()
    val byViewer = ConcurrentHashMap<UUID, T>()

    fun getViewer(viewer: UUID): T? = byViewer[viewer]

    @Synchronized
    fun register(value: T): T? {
        val owner = ownerId(value)
        val viewer = viewerId(value)
        val previous = byViewer.put(viewer, value)
        if (previous != null && previous !== value) remove(previous)
        byViewer[viewer] = value
        byOwner.getOrPut(owner) { ConcurrentHashMap() }[viewer] = value
        return previous
    }

    @Synchronized
    fun remove(value: T): Boolean {
        val owner = ownerId(value)
        val viewer = viewerId(value)
        val viewerRemoved = byViewer.remove(viewer, value)
        val ownerValues = byOwner[owner]
        val ownerRemoved = ownerValues?.remove(viewer, value) == true
        if (ownerValues?.isEmpty() == true) byOwner.remove(owner, ownerValues)
        return viewerRemoved || ownerRemoved
    }

    @Synchronized
    fun removePlayer(player: UUID): Set<T> {
        val values = LinkedHashSet<T>()
        byViewer[player]?.let(values::add)
        byOwner[player]?.values?.let(values::addAll)
        values.forEach(::remove)
        return values
    }

    fun values(): Collection<T> = byViewer.values
}
