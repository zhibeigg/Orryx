package org.gitee.orryx.core.kts.configuration

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.kts.*
import org.gitee.orryx.core.kts.resolver.resolveScriptAnnotation
import org.gitee.orryx.core.kts.resolver.resolveScriptStaticDependencies
import taboolib.module.configuration.util.ReloadAwareLazy
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

val jdkHomeConfig by ReloadAwareLazy(Orryx.config) { Orryx.config.getString("Kts.JdkHome")!! }

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
