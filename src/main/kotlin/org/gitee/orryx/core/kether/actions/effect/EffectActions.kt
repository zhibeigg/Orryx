package org.gitee.orryx.core.kether.actions.effect

import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.platform.ProxyParticle
import taboolib.common.platform.function.adaptLocation
import taboolib.common5.cint
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.awt.Color

object EffectActions {

    /**
     * ```
     * set m matrix
     * effect create/new e {
     *   draw particle "@type DRAGON_BREATH @color 0 0 0 @count 1"
     *   draw matrix &m
     * }
     * effect show &e they "@self" viewer "@self" onHit {
     *   tell &target
     * }
     *
     * or
     *
     * set m matrix
     *
     * effect show effect temp {
     *   draw particle "@type DRAGON_BREATH @color 0 0 0 @count 1"
     *   draw matrix &m
     * } duration 20 period 2 they "@self" viewer "@self" onHit {
     *   tell &target
     * }
     * ```
     * */
    @KetherParser(["effect"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun effect() = scriptParser(
        arrayOf(
            Action.new("Effect粒子效果", "显示粒子", "effect", true)
                .description("显示一次粒子")
                .addEntry("显示占位符", Type.SYMBOL, head = "show")
                .addEntry("粒子效果构建器", Type.EFFECT)
                .addEntry("粒子显示时长，默认单次", Type.LONG, true, "1", "duration")
                .addEntry("粒子显示周期", Type.LONG, true, "1", "period")
                .addContainerEntry("粒子显示位置", true, default = "@self")
                .addContainerEntry("粒子可视者", true, default = "@world", head = "viewer")
                .addEntry("当粒子击中实体时触发", Type.ANY, true, head = "onHit")
                .result("粒子生成器", Type.EFFECT_SPAWNER),
            Action.new("Effect粒子效果", "创建临时特效构建器", "effect", true)
                .description("创建临时特效构建器")
                .addEntry("临时占位符", Type.SYMBOL, head = "temp")
                .addEntry("画板语句", Type.ANY)
                .result("粒子生成器", Type.EFFECT),
            Action.new("Effect粒子效果", "创建指定名特效构建器", "effect", true)
                .description("创建指定名特效构建器，并存储到键名中")
                .addEntry("创建占位符", Type.SYMBOL, head = "create/new")
                .addEntry("画板语句", Type.ANY)
                .result("粒子生成器", Type.EFFECT),
        )
    ) {
        it.switch {
            case("show") {
                show(this)
            }
            case("temp") {
                show(this)
            }
            case("create", "new") {
                show(this)
            }
            other { show(this) }
        }
    }

    @KetherParser(["draw"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun draw() = scriptParser(
        arrayOf(
            Action.new("Effect粒子效果", "设置粒子参数", "draw", true)
                .description("设置粒子基础参数")
                .addEntry("粒子占位符", Type.SYMBOL, head = "particle")
                .addEntry("粒子基础参数", Type.STRING)
                .result("无返回值", Type.NULL),
            Action.new("Effect粒子效果", "设置贝塞尔曲线途经点", "draw", true)
                .description("设置贝塞尔曲线途经点")
                .addEntry("途经点占位符", Type.SYMBOL, head = "locations")
                .addContainerEntry("贝塞尔曲线途经点")
                .result("无返回值", Type.NULL),
            Action.new("Effect粒子效果", "设置矩阵", "draw", true)
                .description("设置矩阵")
                .addEntry("矩阵", Type.SYMBOL, head = "matrix")
                .addEntry("矩阵", Type.MATRIX)
                .result("无返回值", Type.NULL),
            Action.new("Effect粒子效果", "设置红石粒子数据", "draw", true)
                .description("设置红石粒子数据")
                .addEntry("红石数据占位符", Type.SYMBOL, head = "dustData")
                .addEntry("粒子颜色color \"255 255 255\"", Type.STRING)
                .addEntry("粒子大小size", Type.FLOAT)
                .result("无返回值", Type.NULL),
            Action.new("Effect粒子效果", "设置DustTransitionData", "draw", true)
                .description("设置DustTransitionData")
                .addEntry("占位符", Type.SYMBOL, head = "dustTransitionData")
                .addEntry("粒子颜色color \"255 255 255\"", Type.STRING)
                .addEntry("粒子颜色toColor \"255 255 255\"", Type.STRING)
                .addEntry("粒子大小size", Type.FLOAT)
                .result("无返回值", Type.NULL),
            Action.new("Effect粒子效果", "设置ItemData", "draw", true)
                .description("设置ItemData")
                .addEntry("占位符", Type.SYMBOL, head = "itemData")
                .addEntry("材质名", Type.STRING)
                .addEntry("data", Type.INT)
                .addEntry("物品名字", Type.STRING)
                .addEntry("物品描述", Type.STRING)
                .addEntry("customModelData", Type.INT)
                .result("无返回值", Type.NULL),
            Action.new("Effect粒子效果", "设置BlockData", "draw", true)
                .description("设置BlockData")
                .addEntry("占位符", Type.SYMBOL, head = "blockData")
                .addEntry("材质名", Type.STRING)
                .addEntry("data", Type.INT)
                .result("无返回值", Type.NULL),
            Action.new("Effect粒子效果", "设置VibrationData", "draw", true)
                .description("设置VibrationData")
                .addEntry("占位符", Type.SYMBOL, head = "vibrationData")
                .addEntry("origin", Type.CONTAINER)
                .addEntry("到达时间", Type.INT)
                .addContainerEntry("目标", true, default = "@self")
                .result("无返回值", Type.NULL)
        )
    ) {
        it.switch {
            case("particle") {
                drawParticle(this)
            }
            case("matrix") {
                drawMatrix(this)
            }
            case("dustData") {
                drawDustData(this)
            }
            case("dustTransitionData") {
                drawDustTransitionData(this)
            }
            case("itemData") {
                drawItemData(this)
            }
            case("blockData") {
                drawBlockData(this)
            }
            case("vibrationData") {
                drawVibrationData(this)
            }
            case("locations") {
                drawLocation(this)
            }
            other { drawParticle(this) }
        }
    }

    private fun drawMatrix(reader: QuestReader): ScriptAction<Any?> {
        val matrix = reader.nextParsedAction()
        return actionNow {
            run(matrix).matrix { matrix ->
                val effectBuilder = effectBuilder() ?: return@matrix
                effectBuilder.matrix = matrix
            }
        }
    }

    private fun drawDustData(reader: QuestReader): ScriptAction<Any?> {
        val color = reader.nextParsedAction()
        val size = reader.nextParsedAction()
        return actionNow {
            run(color).str { color ->
                run(size).float { size ->
                    val data = color.split(" ")
                    val effectBuilder = effectBuilder() ?: return@float
                    effectBuilder.dustData = ProxyParticle.DustData(Color(data[0].cint, data[1].cint, data[2].cint), size)
                }
            }
        }
    }

    private fun drawDustTransitionData(reader: QuestReader): ScriptAction<Any?> {
        val color = reader.nextParsedAction()
        val toColor = reader.nextParsedAction()
        val size = reader.nextParsedAction()
        return actionNow {
            run(color).str { color ->
                run(toColor).str { toColor ->
                    run(size).float { size ->
                        val data = color.split(" ")
                        val toData = toColor.split(" ")
                        val effectBuilder = effectBuilder() ?: return@float
                        effectBuilder.dustTransitionData =
                            ProxyParticle.DustTransitionData(
                                Color(data[0].cint, data[1].cint, data[2].cint),
                                Color(toData[0].cint, toData[1].cint, toData[2].cint),
                                size
                            )
                    }
                }
            }
        }
    }

    private fun drawItemData(reader: QuestReader): ScriptAction<Any?> {
        val material = reader.nextParsedAction()
        val data = reader.nextParsedAction()
        val name = reader.nextParsedAction()
        val lore = reader.nextParsedAction()
        val customModelData = reader.nextParsedAction()
        return actionNow {
            run(material).str { material ->
                run(data).int { data ->
                    run(name).str { name ->
                        run(lore).str { lore ->
                            run(customModelData).int end@{ customModelData ->
                                val effectBuilder = effectBuilder() ?: return@end
                                effectBuilder.itemData = ProxyParticle.ItemData(material, data, name, listOf(lore), customModelData)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun drawBlockData(reader: QuestReader): ScriptAction<Any?> {
        val material = reader.nextParsedAction()
        val data = reader.nextParsedAction()
        return actionNow {
            run(material).str { material ->
                run(data).int { data ->
                    val effectBuilder = effectBuilder() ?: return@int
                    effectBuilder.blockData = ProxyParticle.BlockData(material, data)
                }
            }
        }
    }

    private fun drawVibrationData(reader: QuestReader): ScriptAction<Any?> {
        val origin = reader.nextParsedAction()
        val arrivalTime = reader.nextParsedAction()
        val destination = reader.nextTheyContainer()
        return actionNow {
            container(origin, self()) { origin ->
                run(arrivalTime).int { arrivalTime ->
                    container(destination, self()) {
                        val effectBuilder = effectBuilder() ?: return@container
                        val des = it.firstInstanceOrNull<ITargetEntity<*>>()?.entity?.uniqueId?.let { uuid -> ProxyParticle.VibrationData.EntityDestination(uuid) } ?: ProxyParticle.VibrationData.LocationDestination(adaptLocation(it.firstInstance<ITargetLocation<*>>().location))
                        effectBuilder.vibrationData = ProxyParticle.VibrationData(
                            adaptLocation(origin.firstInstance<ITargetLocation<*>>().location),
                            des,
                            arrivalTime
                        )
                    }
                }
            }
        }
    }

    private fun drawLocation(reader: QuestReader): ScriptAction<Any?> {
        val they = reader.nextTheyContainer()
        return actionNow {
            container(they, Container()) {
                val effectBuilder = effectBuilder() ?: return@container
                var index = 0
                it.forEachInstance<ITargetLocation<*>> { target ->
                    effectBuilder.locations.add(index to target)
                    index ++
                }
            }
        }
    }

    private fun drawParticle(reader: QuestReader): ScriptAction<Any?> {
        val data = reader.nextParsedAction()
        return actionNow {
            run(data).str { data ->
                val effectBuilder = effectBuilder() ?: return@str
                StringParser(data).entries.forEach {
                    val head = it.head.lowercase()
                    when(head) {
                        "step" -> effectBuilder.step = it.read(0, 0.2)
                        "period" -> effectBuilder.period = it.read(0, 1)
                        "count" -> effectBuilder.count = it.read(0, 1)
                        "speed" -> effectBuilder.speed = it.read(0, 0.0)
                        "particle" -> effectBuilder.particle = it.read(0, ProxyParticle.DUST)
                        "type" -> effectBuilder.type = it.read(0, EffectType.ARC)
                        "startangle" -> effectBuilder.startAngle = it.read(0, 0.0)
                        "angle" -> effectBuilder.angle = it.read(0, 30.0)
                        "radius" -> effectBuilder.radius = it.read(0, 1.0)
                        "sample" -> effectBuilder.sample = it.read(0, 100)
                        "width" -> effectBuilder.width = it.read(0, 1.0)
                        "height" -> effectBuilder.height = it.read(0, 1.0)
                        "length" -> effectBuilder.length = it.read(0, 1.0)
                        "xscalerate" -> effectBuilder.xScaleRate = it.read(0, 1.0)
                        "yscalerate" -> effectBuilder.yScaleRate = it.read(0, 1.0)
                        "vector" -> effectBuilder.vector = it.read(0, null)
                        "corner" -> effectBuilder.corner = it.read(0, 5)
                        "side" -> effectBuilder.side = it.read(0, 3)
                        "maxlength" -> effectBuilder.maxLength = it.read(0, 1.0)
                        "range" -> effectBuilder.range = it.read(0, 1.0)
                    }
                }
            }
        }
    }

    private fun show(reader: QuestReader): ScriptAction<Any?> {
        val effect = reader.nextParsedAction()
        val duration = reader.nextParsedAction()
        val tick = reader.nextParsedAction()
        val they = reader.nextTheyContainer()
        val viewer = reader.nextHeadActionOrNull(arrayOf("viewer"))
        return actionFuture { future ->
            run(effect).effect { effect ->
                run(duration).long { duration ->
                    run(tick).long { period ->
                        containerOrSelf(they) { origins ->
                            container(viewer, worldPlayerWorldContainer(script().bukkitPlayer().world)) { viewers ->
                                future.complete(EffectSpawner(
                                    effect,
                                    duration,
                                    period,
                                    origins,
                                    viewers
                                ))
                            }
                        }
                    }
                }
            }
        }
    }


}