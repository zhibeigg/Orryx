package org.gitee.orryx.core.kether.actions.game.projectile

import org.gitee.orryx.core.kether.ScriptManager.addOrryxCloseable
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.*

object ProjectileAction {

    /**
     * 抛射物 用法
     *
     * 抛射物在每 period 后计算一次 vector 来决定下次的位置
     * ```
     * // 无实体自定义碰撞箱抛射物，客户端同步渲染碰撞箱
     * set a to entity none timeout 20
     * set b to hitbox obb 1 1 1 they &a
     *
     * projectile none vector 1 0 0 &b onHit {
     *   tell &hitLocation
     *   tell &hitEntity
     * } onPeriod {
     *   tell &projectile
     * } period 5 timeout 100 they &a
     *
     * sleep 100
     *
     * // bukkit 实体抛射物
     * set a to entity spawn 金色飞剑 ARMOR_STAND health 10 timeout 20
     * set b to hitbox obb 1 1 1 they &a
     * projectile entity vector 1 0 0 &b onHit {
     *   tell &@hitBlock
     *   tell &@hitEntity
     * } onPeriod {
     *   tell &@ticked
     * } period 1 timeout 20 they &a
     *
     * sleep 20
     *
     * // ady 实体抛射物
     * set a to entity ady 金色飞剑 ARMOR_STAND timeout 20
     * set v to vector 1 0 0
     *
     * projectile ady &v onHit {
     *   tell &hitLocation
     *   tell &hitEntity
     * } onPeriod {
     *   set v to vector &tick 0 0
     *   tell &projectile
     * } period 5 timeout 100 they  &a
     *
     * sleep 40
     *
     * entity remove they &a
     * ```
     * */
    @KetherParser(["projectile"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionLookAt() = scriptParser(
        Action.new("Projectile抛射物", "生成一个无实体抛射物", "projectile", true)
            .description("生成一个无实体抛射物")
            .addEntry("抛射物类型", Type.SYMBOL, head = "none")
            .addEntry("下一Tick前进的方向", Type.VECTOR, false)
            .addEntry("碰撞箱", Type.HITBOX, false)
            .addEntry("碰撞时执行", Type.ANY, true, head = "onHit")
            .addEntry("每周期执行", Type.ANY, true, head = "onPeriod")
            .addEntry("周期", Type.LONG, true, "1", head = "period/p")
            .addEntry("存活时间", Type.LONG, true, "0", head = "timeout/t")
            .addEntry("是否与实体碰撞", Type.BOOLEAN, true, "true", head = "hitEntity/he")
            .addEntry("是否与方块碰撞", Type.BOOLEAN, true, "false", head = "hitBlock/hb")
            .addEntry("是否可以穿透方块", Type.BOOLEAN, true, "false", head = "through/th")
            .addContainerEntry("生成位置", optional = true, default = "@self")
            .result("抛射物", Type.TARGET)
    ) {
        val type = Projectile.ProjectileType.parseIgnoreCase(it.nextToken())
        val vector = it.nextParsedAction()
        val hitbox = it.nextParsedAction()
        val onHit = it.nextHeadActionOrNull("onHit")
        val onPeriod = it.nextHeadActionOrNull("onPeriod")
        val period = it.nextHeadAction("period", "p", def = 1)
        val timeout = it.nextHeadAction("timeout", "t", def = 0)
        val hitEntity = it.nextHeadAction("hitEntity", "he", def = true)
        val hitBlock = it.nextHeadAction("hitBlock", "hb", def = false)
        val through = it.nextHeadAction("through", "th", def = false)
        val they = it.nextTheyContainerOrNull()

        actionFuture { future ->
            run(hitbox).collider { hitbox ->
                run(period).long { period ->
                    run(timeout).long { timeout ->
                        run(hitEntity).bool { hitEntity ->
                            run(hitBlock).bool { hitBlock ->
                                run(through).bool { through ->
                                    container(they, self()) { container ->
                                        val location = container.firstInstance<ITargetLocation<*>>()
                                        val projectile = Projectile(
                                            type = type,
                                            period = period,
                                            timeout = timeout,
                                            parent = location,
                                            hitbox = hitbox,
                                            vector = vector,
                                            onHit = onHit,
                                            onPeriod = onPeriod,
                                            hitBlock = hitBlock,
                                            hitEntity = hitEntity,
                                            through = through
                                        )
                                        addOrryxCloseable(projectile.removed) {
                                            ensureSync { projectile.remove() }
                                        }
                                        projectile.start(this)
                                        future.complete(projectile)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}