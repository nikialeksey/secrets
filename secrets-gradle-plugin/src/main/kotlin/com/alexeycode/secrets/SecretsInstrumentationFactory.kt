package com.alexeycode.secrets

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor

abstract class SecretsInstrumentationFactory : AsmClassVisitorFactory<SecretsInstrumentationFactory.SecretsParams> {

    interface SecretsParams : InstrumentationParameters {
        @get:Input
        val secrets: MapProperty<String, String>
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        val secrets = this.parameters.get().secrets.get()
        return SecretsClassInflator(secrets, nextClassVisitor)
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.className == "com.alexeycode.secrets.Secrets"
    }
}