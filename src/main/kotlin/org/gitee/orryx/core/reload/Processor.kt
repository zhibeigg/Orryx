package org.gitee.orryx.core.reload

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


@SupportedAnnotationTypes("org.gitee.orryx.core.reload.Reload")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class Processor: AbstractProcessor() {

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (element in roundEnv.getElementsAnnotatedWith(Reload::class.java)) {
            if (!element.kind.isClass) continue
            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, element.simpleName.toString())
        }
        return true // 表示此处理器已经声明了它处理的注解
    }

}