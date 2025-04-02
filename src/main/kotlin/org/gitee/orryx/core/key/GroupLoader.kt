package org.gitee.orryx.core.key

class GroupLoader(override val key: String): IGroup {

    override fun toString(): String {
        return "GroupLoader(key=$key)"
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is GroupLoader) {
            key == other.key
        } else {
            false
        }
    }

}