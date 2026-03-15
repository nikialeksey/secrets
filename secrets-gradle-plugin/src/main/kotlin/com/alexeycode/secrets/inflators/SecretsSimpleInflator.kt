package com.alexeycode.secrets.inflators

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class SecretsSimpleInflator(
    private val secrets: Map<String, String>,
    nextClassVisitor: ClassVisitor
) : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {

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

                    override fun visitInsn(opcode: Int) { }
                    override fun visitIntInsn(opcode: Int, operand: Int) {}
                    override fun visitVarInsn(opcode: Int, `var`: Int) {}
                    override fun visitTypeInsn(opcode: Int, type: String?) {}
                    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {}
                    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {}
                    override fun visitJumpInsn(opcode: Int, label: Label?) {}
                    override fun visitLdcInsn(value: Any?) {}
                    override fun visitIincInsn(`var`: Int, increment: Int) {}
                    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label?, vararg labels: Label?) {}
                    override fun visitLookupSwitchInsn(dflt: Label?, keys: IntArray?, labels: Array<out Label>?) {}
                    override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {}

                    override fun visitEnd() {
                        mv.visitCode()

                        mv.visitLdcInsn(value)
                        mv.visitInsn(Opcodes.ARETURN)

                        mv.visitMaxs(1, 0)
                        super.visitEnd()
                    }
                }
            }
        }

        return mv
    }
}