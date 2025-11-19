/**
 * 示例脚本 5: 粒子效果
 *
 * 演示粒子效果和定时任务
 */

import org.bukkit.Particle
import org.bukkit.scheduler.BukkitRunnable
import kotlin.math.cos
import kotlin.math.sin

player?.let { p ->
    // 获取参数
    val particleType = context["particle"] as? String ?: "FLAME"
    val count = context["count"] as? Int ?: 10
    val radius = context["radius"] as? Double ?: 1.0
    val duration = context["duration"] as? Int ?: 20 // ticks

    // 解析粒子类型
    val particle = try {
        Particle.valueOf(particleType.uppercase())
    } catch (e: IllegalArgumentException) {
        p.sendMessage("§c无效的粒子类型: $particleType")
        return@let "粒子效果失败"
    }

    // 创建粒子效果任务
    var tickCount = 0
    object : BukkitRunnable() {
        override fun run() {
            if (tickCount >= duration || !p.isOnline) {
                cancel()
                return
            }

            val loc = p.location.add(0.0, 1.0, 0.0)

            // 生成圆形粒子
            for (i in 0 until count) {
                val angle = 2 * Math.PI * i / count
                val x = radius * cos(angle)
                val z = radius * sin(angle)

                val particleLoc = loc.clone().add(x, 0.0, z)
                p.world.spawnParticle(particle, particleLoc, 1, 0.0, 0.0, 0.0, 0.0)
            }

            tickCount++
        }
    }.runTaskTimer(plugin, 0L, 1L)

    p.sendMessage("§a已生成粒子效果: $particleType")
    "粒子效果已启动"
} ?: "此脚本需要玩家执行"
