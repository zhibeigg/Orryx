package org.gitee.orryx.core.kts.configuration

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.kts.DependPlugin
import org.gitee.orryx.core.kts.Import
import org.gitee.orryx.core.kts.Script
import org.gitee.orryx.core.kts.bukkitImports
import org.gitee.orryx.core.kts.javaImports
import org.gitee.orryx.core.kts.kotlinCoroutinesImports
import org.gitee.orryx.core.kts.kotlinImports
import org.gitee.orryx.core.kts.resolver.resolveScriptAnnotation
import org.gitee.orryx.core.kts.resolver.resolveScriptStaticDependencies
import org.gitee.orryx.core.kts.scriptingImports
import org.gitee.orryx.utils.ConfigLazy
import java.io.File
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

val jdkHomeConfig: T by ConfigLazy { Orryx.config.getString("Kts.JdkHome")!! }

class KtsScriptCompilationConfiguration : ScriptCompilationConfiguration({
    defaultImports(
        bukkitImports + kotlinImports + javaImports + kotlinCoroutinesImports + scriptingImports,
    )
    jvm {
        jdkHome(File(jdkHomeConfig))

        dependenciesFromClassContext(KtsScriptCompilationConfiguration::class, wholeClasspath = true)
        compilerOptions(
            // "-Xopt-in=kotlin.time.ExperimentalTime,kotlin.ExperimentalStdlibApi,kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-jvm-target",
            "17",
            "-api-version",
            "1.8",
        )
    }
    refineConfiguration {
        beforeCompiling(handler = ::resolveScriptStaticDependencies)
        onAnnotations(
            listOf(Script::class, DependPlugin::class, Import::class, DependsOn::class, Repository::class),
            handler = ::resolveScriptAnnotation,
        )
    }
    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
})
