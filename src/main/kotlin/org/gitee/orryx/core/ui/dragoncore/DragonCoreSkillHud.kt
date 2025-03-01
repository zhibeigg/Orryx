package org.gitee.orryx.core.ui.dragoncore

import org.bukkit.entity.Player
import org.gitee.orryx.core.ui.AbstractSkillHud
import org.gitee.orryx.core.ui.germplugin.GermPluginSkillHud
import java.util.*

class DragonCoreSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    companion object {

        /**
         * owner, viewer, [GermPluginSkillHud]
         */
        internal val dragonSkillHudMap by lazy { hashMapOf<UUID, MutableMap<UUID, DragonCoreSkillHud>>() }

    }

    override fun update() {
        TODO("Not yet implemented")
    }

    override fun open() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}