package org.gitee.orryx.compat

import org.gitee.orryx.compat.arcartx.ArcartXKeyRegisterSender
import org.gitee.orryx.compat.dragoncore.DragonCoreKeyRegisterSender
import org.gitee.orryx.compat.germplugin.GermKeyRegisterSender
import org.gitee.orryx.utils.ArcartXPlugin
import org.gitee.orryx.utils.DragonCorePlugin
import org.gitee.orryx.utils.GermPluginPlugin

/**
 * 按键注册发送器管理器。
 *
 * 根据当前启用的客户端模组自动选择对应的发送器。
 */
object KeyRegisterSenderManager {

    private val cachedSender: IKeyRegisterSender? by lazy {
        when {
            GermPluginPlugin.isEnabled -> GermKeyRegisterSender()
            DragonCorePlugin.isEnabled -> DragonCoreKeyRegisterSender()
            ArcartXPlugin.isEnabled -> ArcartXKeyRegisterSender()
            else -> null
        }
    }

    /**
     * 获取当前可用的按键注册发送器。
     *
     * @return 发送器实例，如果没有可用的客户端模组则返回 null
     */
    fun getSender(): IKeyRegisterSender? = cachedSender
}
