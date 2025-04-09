package org.gitee.orryx.core.kether.actions.effect

import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.kether.ScriptManager.addOrryxCloseable
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.adaptLocation
import taboolib.common5.cint
import taboolib.library.kether.QuestReader
import taboolib.library.xseries.XParticle
import taboolib.module.kether.*
import java.awt.Color

object EffectActions {

    /**
     * ```
     * set m matrix
     * effect create/new e {
     *   draw particle "@type DRAGON_BREATH @count 1"
     *   draw matrix &m
     * }
     * effect show &e they "@self" viewer "@self"
     *
     * or
     *
     * set m matrix
     *
     * effect show effect temp {
     *   draw particle "@type DRAGON_BREATH @count 1"
     *   draw matrix &m
     * } duration 20 period 2 they "@self" viewer "@self"
     * ```
     * */
    @KetherParser(["effect"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun effect() = scriptParser(
        arrayOf(
            Action.new("Effect粒子效果", "显示粒子", "effect", true)
                .description("创建粒子生成器并显示粒子")
                .addEntry("显示占位符", Type.SYMBOL, head = "show")
                .addEntry("粒子效果构建器", Type.EFFECT)
                .addEntry("粒子显示时长，默认单次", Type.LONG, true, "1", "duration")
                .addEntry("粒子显示周期", Type.LONG, true, "1", "period")
                .addContainerEntry("粒子显示位置", true, default = "@self")
                .addContainerEntry("粒子可视者", true, default = "@world", head = "viewer")
                .result("粒子生成器", Type.EFFECT_SPAWNER),
            Action.new("Effect粒子效果", "停止显示粒子", "effect", true)
                .description("停止显示粒子")
                .addEntry("停止显示占位符", Type.SYMBOL, head = "stop")
                .addEntry("粒子生成器", Type.EFFECT_SPAWNER)
                .result("粒子生成器", Type.EFFECT_SPAWNER),
            Action.new("Effect粒子效果", "创建临时粒子效果构建器", "effect", true)
                .description("创建临时粒子效果构建器")
                .addEntry("临时占位符", Type.SYMBOL, head = "temp")
                .addEntry("画板语句", Type.ANY)
                .result("粒子效果构建器", Type.EFFECT),
            Action.new("Effect粒子效果", "创建指定名粒子效果构建器", "effect", true)
                .description("创建指定名粒子效果构建器，并存储到键名中")
                .addEntry("创建占位符", Type.SYMBOL, head = "create/new")
                .addEntry("画板语句", Type.ANY)
                .result("粒子效果构建器", Type.EFFECT),
            Action.new("Effect粒子效果", "微调粒子效果构建器", "effect", true)
                .description("微调粒子效果构建器")
                .addEntry("微调占位符", Type.SYMBOL, head = "trim")
                .addEntry("特效构建器", Type.EFFECT)
                .addEntry("画板语句", Type.ANY)
                .result("粒子效果构建器", Type.EFFECT)
        )
    ) {
        it.switch {
            case("show") {
                show(this)
            }
            case("stop") {
                stop(this)
            }
            case("temp") {
                temp(this)
            }
            case("trim") {
                trim(this)
            }
            case("create", "new") {
                create(this)
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
            Action.new("Effect粒子效果", "设置变换矩阵", "draw", true)
                .description("设置变换矩阵")
                .addEntry("矩阵变换占位符", Type.SYMBOL, head = "transform")
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
                .result("无返回值", Type.NULL),
            Action.new("Effect粒子效果", "设置偏移向量", "draw", true)
                .description("设置偏移向量")
                .addEntry("占位符", Type.SYMBOL, head = "offset")
                .addEntry("vector", Type.VECTOR)
                .result("无返回值", Type.NULL),
            Action.new("Effect粒子效果", "设置位移向量", "draw", true)
                .description("设置位移向量")
                .addEntry("占位符", Type.SYMBOL, head = "translate")
                .addEntry("vector", Type.VECTOR)
                .result("无返回值", Type.NULL)
        )
    ) {
        it.switch {
            case("particle") {
                drawParticle(this)
            }
            case("offset") {
                drawOffset(this)
            }
            case("translate") {
                drawTranslate(this)
            }
            case("transform") {
                drawTransform(this)
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

    private fun drawTranslate(reader: QuestReader): ScriptAction<Any?> {
        val vector = reader.nextParsedAction()
        return actionNow {
            run(vector).vector { vector ->
                val effectBuilder = effectBuilder() ?: return@vector
                effectBuilder.translate = vector
            }
        }
    }

    private fun drawOffset(reader: QuestReader): ScriptAction<Any?> {
        val vector = reader.nextParsedAction()
        return actionNow {
            run(vector).vector { vector ->
                val effectBuilder = effectBuilder() ?: return@vector
                effectBuilder.offset = vector
            }
        }
    }

    private fun drawTransform(reader: QuestReader): ScriptAction<Any?> {
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
                    effectBuilder.dustData = ParticleData.DustData(Color(data[0].cint, data[1].cint, data[2].cint), size)
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
                            ParticleData.DustTransitionData(
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
                                effectBuilder.itemData = ParticleData.ItemData(material, data, name, listOf(lore), customModelData)
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
                    effectBuilder.blockData = ParticleData.BlockData(material, data)
                }
            }
        }
    }

    private fun drawVibrationData(reader: QuestReader): ScriptAction<Any?> {
        val origin = reader.nextParsedAction()
        val arrivalTime = reader.nextParsedAction()
        val destination = reader.nextTheyContainerOrNull()
        return actionNow {
            container(origin, self()) { origin ->
                run(arrivalTime).int { arrivalTime ->
                    container(destination, self()) end@{
                        val effectBuilder = effectBuilder() ?: return@end
                        val des = it.firstInstanceOrNull<ITargetEntity<*>>()?.entity?.uniqueId?.let { uuid -> ParticleData.VibrationData.EntityDestination(uuid) } ?: ParticleData.VibrationData.LocationDestination(adaptLocation(it.firstInstance<ITargetLocation<*>>().location))
                        effectBuilder.vibrationData = ParticleData.VibrationData(
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
        val they = reader.nextTheyContainerOrNull()
        return actionNow {
            container(they, Container()) {
                val effectBuilder = effectBuilder() ?: return@container
                var index = 0
                it.forEachInstance<ITargetLocation<*>> { target ->
                    effectBuilder.locations.add(index to EffectOrigin(target))
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
                        "particle" -> effectBuilder.particle = it.read(0, XParticle.DUST)
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
        val mode = reader.nextHeadAction("mode", "show")
        val they = reader.nextTheyContainerOrNull()
        val viewer = reader.nextHeadActionOrNull(arrayOf("viewer"))
        return actionFuture { future ->
            run(effect).effect { effect ->
                run(duration).long { duration ->
                    run(tick).long { period ->
                        run(mode).str { mode ->
                            containerOrSelf(they) { origins ->
                                container(viewer, worldPlayerWorldContainer(script().bukkitPlayer().world)) { viewers ->
                                    val spawner = EffectSpawner(
                                        effect,
                                        duration,
                                        period,
                                        SpawnerType.valueOf(mode.uppercase()),
                                        origins,
                                        viewers
                                    )
                                    addOrryxCloseable(spawner.future) {
                                        spawner.stop()
                                    }
                                    spawner.start()
                                    future.complete(spawner)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun trim(reader: QuestReader): ScriptAction<Any?> {
        val effect = reader.nextParsedAction()
        val function = reader.nextParsedAction()
        return actionFuture { future ->
            run(effect).effect { effect ->
                script()["@effect"] = effect
                run(function).whenComplete { _, _ ->
                    script()["@effect"] = null
                    future.complete(effect)
                }
            }
        }
    }

    private fun stop(reader: QuestReader): ScriptAction<Any?> {
        val spawner = reader.nextParsedAction()
        return actionFuture { future ->
            run(spawner).effectSpawner { spawner ->
                spawner.stop()
                future.complete(spawner)
            }
        }
    }

    private fun temp(reader: QuestReader): ScriptAction<Any?> {
        val function = reader.nextParsedAction()
        return actionFuture { future ->
            val effect = EffectBuilder()
            script()["@effect"] = effect
            run(function).whenComplete { _, _ ->
                script()["@effect"] = null
                future.complete(effect)
            }
        }
    }

    private fun create(reader: QuestReader): ScriptAction<Any?> {
        val key = reader.nextParsedAction()
        val function = reader.nextParsedAction()
        return actionFuture { future ->
            val effect = EffectBuilder()
            script()["@effect"] = effect
            run(key).str { key ->
                run(function).whenComplete { _, _ ->
                    script()["@effect"] = null
                    script()[key] = effect
                    future.complete(effect)
                }
            }
        }
    }

}