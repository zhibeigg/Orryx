package org.gitee.orryx.core.container

import org.gitee.orryx.core.targets.ITarget

class Container(override val targets: MutableSet<ITarget<*>> = mutableSetOf()): IContainer {

    override fun merge(other: IContainer): IContainer {
        targets += other.targets
        return this
    }

    override infix fun and(other: IContainer): IContainer {
        targets += other.targets
        return this
    }


    override fun remove(other: IContainer): IContainer {
        targets -= other.targets
        return this
    }

    override fun remove(target: ITarget<*>): IContainer {
        targets -= target
        return this
    }

    override fun add(target: ITarget<*>): IContainer {
        targets += target
        return this
    }

    override fun removeIf(func: (target: ITarget<*>) -> Boolean): IContainer {
        targets.removeIf {
            func(it)
        }
        return this
    }

    override fun mergeIf(other: IContainer, func: (target: ITarget<*>) -> Boolean): IContainer {
        other.targets.forEach {
            if (func(it)) {
                targets.add(it)
            }
        }
        return this
    }

    override fun foreach(func: (target: ITarget<*>) -> Unit) {
        targets.forEach {
            func(it)
        }
    }

    override fun addAll(targets: Iterable<ITarget<*>>): IContainer {
        targets.forEach {
            this.targets += it
        }
        return this
    }


    override fun clone(): IContainer = Container().also {
        it.addAll(targets)
    }

    override fun toString(): String {
        return targets.map { it.getSource().toString() }.toString()
    }

}