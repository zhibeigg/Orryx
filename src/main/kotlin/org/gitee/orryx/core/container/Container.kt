package org.gitee.orryx.core.container

import org.gitee.orryx.core.targets.ITarget
import kotlin.collections.LinkedHashSet

class Container(override val targets: LinkedHashSet<ITarget<*>> = linkedSetOf()) : IContainer {

    override fun merge(other: IContainer): IContainer = apply {
        targets.addAll(other.targets)
    }

    override infix fun and(other: IContainer): IContainer = merge(other)

    override fun remove(other: IContainer): IContainer = apply {
        targets.removeAll(other.targets)
    }

    override fun remove(target: ITarget<*>): IContainer = apply {
        targets.remove(target)
    }

    override fun add(target: ITarget<*>): IContainer = apply {
        targets.add(target)
    }

    override fun addAll(targets: Iterable<ITarget<*>>): IContainer = apply {
        this.targets.addAll(targets)
    }

    override fun removeIf(predicate: TargetPredicate): IContainer = apply {
        targets.removeIf { predicate.test(it) }
    }

    override fun mergeIf(other: IContainer, predicate: TargetPredicate): IContainer = apply {
        targets.addAll(other.targets.filter { predicate.test(it) })
    }

    override fun foreach(action: TargetConsumer) {
        targets.forEach { action.accept(it) }
    }

    override fun clone(): IContainer = Container(LinkedHashSet(targets))

    override fun first(): ITarget<*> = targets.first()

    override fun firstOrNull(): ITarget<*>? = targets.firstOrNull()

    override fun take(amount: Int): List<ITarget<*>> = targets.take(amount)

    override fun drop(amount: Int): List<ITarget<*>> = targets.drop(amount)

    override fun toString(): String {
        return targets.joinToString(
            prefix = "[",
            postfix = "]",
            transform = { it.getSource().toString() }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Container
        return targets == other.targets
    }

    override fun hashCode(): Int = targets.hashCode()
}
