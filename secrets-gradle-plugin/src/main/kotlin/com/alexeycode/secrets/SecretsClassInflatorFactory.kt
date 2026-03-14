package com.alexeycode.secrets

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor

abstract class SecretsClassInflatorFactory : AsmClassVisitorFactory<SecretsClassInflatorFactory.SecretsParams> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        val params = this.parameters.get()
        val secrets = params.secrets.get()
        val className = parameters.get().className.get()
        return SecretsClassInflator(secrets, className, nextClassVisitor)
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.className == parameters.get().inflatableClassName.get()
    }

    interface SecretsParams : InstrumentationParameters {
        @get:Input
        val secrets: MapProperty<String, String>

        @get:Input
        val className: Property<String>

        @get:Input
        val inflatableClassName: Property<String>
    }
}