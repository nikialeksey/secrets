package com.alexeycode.secrets

import com.alexeycode.secrets.inflators.SecretsNevilleInflator
import com.alexeycode.secrets.inflators.SecretsSimpleInflator
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
        val params = parameters.get()
        val secrets = params.secrets.get()
        val className = params.className.get()
        val method = params.method.get()
        return when (method) {
            SecretsHardeningMethod.NONE -> SecretsSimpleInflator(secrets, nextClassVisitor)
            SecretsHardeningMethod.NEVILLE -> SecretsNevilleInflator(secrets, className, nextClassVisitor)
        }
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

        @get:Input
        val method: Property<SecretsHardeningMethod>
    }
}