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
            private var clinitVisited = false

            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor? {
                var mv = super.visitMethod(access, name, descriptor, signature, exceptions)

                if (name == "<clinit>") {
                    clinitVisited = true
                    mv = object : MethodVisitor(Opcodes.ASM9, mv) {
                        override fun visitCode() {
                            super.visitCode()
                            injectSecrets(this, secrets)
                        }
                    }
                }
                return mv
            }

            override fun visitEnd() {
                if (!clinitVisited) {
                    val mv = cv.visitMethod(
                        Opcodes.ACC_STATIC,
                        "<clinit>",
                        "()V",
                        null,
                        null
                    )
                    mv.visitCode()
                    injectSecrets(mv, secrets)
                    mv.visitInsn(Opcodes.RETURN)
                    mv.visitMaxs(1, 0)
                    mv.visitEnd()
                }
                super.visitEnd()
            }

            private fun injectSecrets(mv: MethodVisitor, secrets: Map<String, String>) {
                for ((key, value) in secrets) {
                    mv.visitLdcInsn(value)

                    mv.visitFieldInsn(
                        Opcodes.PUTSTATIC,
                        "com/alexeycode/secrets/Secrets",
                        key,
                        "Ljava/lang/String;"
                    )
                }
            }
        }
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.className == "com.alexeycode.secrets.Secrets"
    }
}