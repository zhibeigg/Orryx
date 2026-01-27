package org.gitee.orryx.compat

import org.bukkit.entity.Player
import org.gitee.orryx.compat.dragoncore.DragonCoreAnimationBridge
import org.gitee.orryx.compat.germplugin.GermPluginAnimationBridge
import org.gitee.orryx.utils.DragonCorePlugin
import org.gitee.orryx.utils.GermPluginPlugin
import taboolib.common.util.unsafeLazy

/**
 * 动画系统兼容桥接接口。
 */
interface IAnimationBridge {

    companion object {

        val INSTANCE by unsafeLazy {
            when {
                DragonCorePlugin.isEnabled -> DragonCoreAnimationBridge()
                GermPluginPlugin.isEnabled -> GermPluginAnimationBridge()
                else -> DefaultAnimationBridge()
            }
        }
    }

    /**
     * 设置玩家动画。
     *
     * @param viewer 观看者
     * @param player 目标玩家
     * @param animation 动画名
     * @param speed 播放速度
     */
    fun setPlayerAnimation(viewer: Player, player: Player, animation: String, speed: Float)

    /**
     * 移除玩家动画。
     *
     * @param viewer 观看者
     * @param player 目标玩家
     * @param animation 动画名
     */
    fun removePlayerAnimation(viewer: Player, player: Player, animation: String)

    /**
     * 清空玩家动画。
     *
     * @param viewer 观看者
     * @param player 目标玩家
     */
    fun clearPlayerAnimation(viewer: Player, player: Player)
}
