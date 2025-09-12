package org.gitee.orryx.module.lang

import org.gitee.orryx.core.reload.Reload
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.lang.Language

object LanguageLoader {

    @Awake(LifeCycle.ENABLE)
    private fun load() {
        Language.languageType["gddtitle_action"] = TypeGDDTitleAction::class.java
        Language.languageType["gddtitleaction"] = TypeGDDTitleAction::class.java
        Language.languageType["gddtitle_title"] = TypeGDDTitleTitle::class.java
        Language.languageType["gddtitletitle"] = TypeGDDTitleTitle::class.java
    }

    @Reload(weight = 0)
    private fun reload() {
        Language.reload()
    }
}