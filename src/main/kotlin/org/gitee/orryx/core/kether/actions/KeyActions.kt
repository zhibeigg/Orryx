package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.keySetting
import org.gitee.orryx.utils.scriptParser
import taboolib.module.kether.*

object KeyActions {

    @KetherParser(["keySetting"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionState() = scriptParser(
        Action.new("KeySetting按键设置", "指向取消按键", "keySetting", true)
            .description("指向取消按键")
            .addEntry("指向取消键占位符", Type.SYMBOL, false, head = "aimCancel")
            .result("按键名", Type.STRING),
        Action.new("KeySetting按键设置", "获取指向确认按键", "keySetting", true)
            .description("获取指向确认按键")
            .addEntry("指向确认键占位符", Type.SYMBOL, false, head = "aimConfirm")
            .result("按键名", Type.STRING),
        Action.new("KeySetting按键设置", "获取普通攻击按键", "keySetting", true)
            .description("获取普通攻击按键")
            .addEntry("普攻键占位符", Type.SYMBOL, false, head = "generalAttack")
            .result("按键名", Type.STRING),
        Action.new("KeySetting按键设置", "获取格挡按键", "keySetting", true)
            .description("获取格挡按键")
            .addEntry("格挡占位符", Type.SYMBOL, false, head = "block")
            .result("按键名", Type.STRING),
        Action.new("KeySetting按键设置", "获取闪避按键", "keySetting", true)
            .description("获取闪避按键")
            .addEntry("闪避占位符", Type.SYMBOL, false, head = "dodge")
            .result("按键名", Type.STRING),
        Action.new("KeySetting按键设置", "获取技能绑定按键", "keySetting", true)
            .description("获取技能绑定按键")
            .addEntry("技能绑定占位符", Type.SYMBOL, false, head = "bind")
            .addEntry("技能绑定键名", Type.STRING, false)
            .result("按键名", Type.STRING),
        Action.new("KeySetting按键设置", "获取拓展按键", "keySetting", true)
            .description("获取拓展按键")
            .addEntry("拓展占位符", Type.SYMBOL, false, head = "extend")
            .addEntry("拓展ID", Type.STRING, false)
            .result("按键名", Type.STRING)
    ) {
        it.switch {
            case("aimCancel") {
                actionTake {
                    keySetting { setting ->
                        setting.aimCancelKey
                    }
                }
            }
            case("aimConfirm") {
                actionTake {
                    keySetting { setting ->
                        setting.aimConfirmKey
                    }
                }
            }
            case("generalAttack") {
                actionTake {
                    keySetting { setting ->
                        setting.generalAttackKey
                    }
                }
            }
            case("block") {
                actionTake {
                    keySetting { setting ->
                        setting.blockKey
                    }
                }
            }
            case("dodge") {
                actionTake {
                    keySetting { setting ->
                        setting.dodgeKey
                    }
                }
            }
            case("bind") {
                val keyBind = nextParsedAction()
                actionFuture { f ->
                    run(keyBind).str { keyBind ->
                        val bind = BindKeyLoaderManager.getBindKey(keyBind)
                        keySetting { setting ->
                            f.complete(setting.bindKeyMap[bind])
                        }
                    }
                }
            }
            case("extend") {
                val id = nextParsedAction()
                actionFuture { f ->
                    run(id).str { id ->
                        keySetting { setting ->
                            f.complete(setting.extKeyMap[id])
                        }
                    }
                }
            }
        }
    }
}