package org.gitee.orryx.dao.pojo

import org.gitee.orryx.core.profile.IFlag
import java.util.*

data class PlayerData(val player: UUID, val job: String?, val point: Int, val flags: Map<String, IFlag<*>>)