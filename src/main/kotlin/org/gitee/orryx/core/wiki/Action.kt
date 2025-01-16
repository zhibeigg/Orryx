package org.gitee.orryx.core.wiki

class Action(val key: String, val sharded: Boolean, val entries: MutableList<Entry> = mutableListOf(), var description: String = "") {

    class Entry(val description: String, val type: Type, val optional: Boolean, val default: String? = null, val head: String? = null)

    var result: Type = Type.VOID

    var resultDescription: String? = null

    companion object {

        fun new(key: String, sharded: Boolean = false): Action {
            return Action(key, sharded)
        }

    }

    fun addEntry(description: String, type: Type, optional: Boolean = false, default: String? = null, head: String? = null): Action {
        entries += Entry(description, type, optional, default, head)
        return this
    }

    fun addContainerEntry(description: String = "目标容器", optional: Boolean = false, default: String? = null, head: String? = "they"): Action {
        entries += Entry(description, Type.CONTAINER, optional, default, head)
        return this
    }

    fun description(description: String): Action {
        this.description = description
        return this
    }

    fun result(description: String, result: Type): Action {
        this.result = result
        this.resultDescription = description
        return this
    }

}