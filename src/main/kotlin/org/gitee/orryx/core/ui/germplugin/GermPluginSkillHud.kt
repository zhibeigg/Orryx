package org.gitee.orryx.core.ui.germplugin

import org.bukkit.entity.Player
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.ui.AbstractSkillHud
import org.gitee.orryx.core.ui.IUIManager
import java.util.*

open class GermPluginSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    companion object {

        /**
         * owner, viewer, [GermPluginSkillHud]
         */
        internal val germSkillHudMap by lazy { mutableMapOf<UUID, MutableMap<UUID, GermPluginSkillHud>>() }

        fun getViewerHud(player: Player): GermPluginSkillHud? {
            return germSkillHudMap.firstNotNullOfOrNull {
                it.value[player.uniqueId]
            }
        }

        @Reload(2)
        private fun reload() {
            if (IUIManager.INSTANCE !is GermPluginUIManager) return
            germSkillHudMap.forEach {
                it.value.forEach { map ->
                    map.value.update()
                }
            }
        }

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