package com.alexeycode.secrets

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

abstract class SecretsClassVisitorFactory : AsmClassVisitorFactory<SecretsClassVisitorFactory.SecretsParams> {

    interface SecretsParams : InstrumentationParameters {
        @get:Input
        val secrets: MapProperty<String, String>
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        val secrets = this.parameters.get().secrets.get()
        return object : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {

            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
                if (name == null) return mv

                if (name.startsWith("get") && descriptor == "()Ljava/lang/String;") {
                    val key = name.removePrefix("get").replaceFirstChar { it.lowercaseChar() }
                    val value = secrets[key]
                    if (value != null) {
                        return object : MethodVisitor(Opcodes.ASM9, mv) {

                            override fun visitCode() {
                                super.visitCode()

                                mv.visitLdcInsn(value)
                                mv.visitInsn(Opcodes.ARETURN)

                                mv.visitMaxs(1, 0)
                                mv.visitEnd()
                            }

                            override fun visitInsn(opcode: Int) {}
                        }
                    }
                }

                return mv
            }
        }
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.className == "com.alexeycode.secrets.Secrets"
    }
}