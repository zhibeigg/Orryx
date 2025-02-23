package org.gitee.orryx.utils

import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey

const val DEFAULT = "default"

fun bindKeys(): List<IBindKey> {
    return BindKeyLoaderManager.getBindKeys().values.sortedBy { it.sort }
}