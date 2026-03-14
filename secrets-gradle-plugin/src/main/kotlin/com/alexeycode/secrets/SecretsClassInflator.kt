package com.alexeycode.secrets

import org.apache.commons.math3.analysis.interpolation.NevilleInterpolator
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class SecretsClassInflator(
    private val secrets: Map<String, String>,
    private val className: String,
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

                val cMethod = name + "C"
                val len = value.length

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

                        val loopStart = Label()
                        val loopEnd = Label()

                        // char[] buf = new char[len]
                        mv.visitIntInsn(Opcodes.BIPUSH, len)
                        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_CHAR)
                        mv.visitVarInsn(Opcodes.ASTORE, 0)

                        // int i = 0
                        mv.visitInsn(Opcodes.ICONST_0)
                        mv.visitVarInsn(Opcodes.ISTORE, 1)

                        mv.visitLabel(loopStart)

                        // if (i >= len) break
                        mv.visitVarInsn(Opcodes.ILOAD, 1)
                        mv.visitIntInsn(Opcodes.BIPUSH, len)
                        mv.visitJumpInsn(Opcodes.IF_ICMPGE, loopEnd)

                        // buf[i] = getXC(i)

                        mv.visitVarInsn(Opcodes.ALOAD, 0)  // buf
                        mv.visitVarInsn(Opcodes.ILOAD, 1)  // i
                        mv.visitVarInsn(Opcodes.ILOAD, 1)  // arg

                        mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            className,
                            cMethod,
                            "(I)C",
                            false
                        )

                        mv.visitInsn(Opcodes.CASTORE)

                        // i++
                        mv.visitIincInsn(1, 1)

                        mv.visitJumpInsn(Opcodes.GOTO, loopStart)

                        mv.visitLabel(loopEnd)

                        // return new String(buf)

                        mv.visitTypeInsn(Opcodes.NEW, "java/lang/String")
                        mv.visitInsn(Opcodes.DUP)
                        mv.visitVarInsn(Opcodes.ALOAD, 0)

                        mv.visitMethodInsn(
                            Opcodes.INVOKESPECIAL,
                            "java/lang/String",
                            "<init>",
                            "([C)V",
                            false
                        )

                        mv.visitInsn(Opcodes.ARETURN)

                        mv.visitMaxs(4, 2)

                        super.visitEnd()
                    }
                }
            }
        }

        return mv
    }

    override fun visitEnd() {
        for ((key, value) in secrets) {
            // Thanks, Joshua!
            // https://x.com/nikialeksey/status/1600598026678149120
            val valueLength = value.length
            val x = DoubleArray(valueLength + 1)
            val y = DoubleArray(valueLength + 1)
            for (i in 0 until valueLength) {
                x[i] = i.toDouble()
                y[i] = value[i].code.toDouble()
            }
            x[valueLength] = valueLength.toDouble()
            y[valueLength] = 0.0

            val interpolator = NevilleInterpolator()
            val function = interpolator.interpolate(x, y)
            val coeff = function.getCoefficients()

            generateCoeffMethod(cv, key, coeff)
        }
        super.visitEnd()
    }

    private fun generateCoeffMethod(
        cv: ClassVisitor,
        name: String,
        coeff: DoubleArray
    ) {
        val mv = cv.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "get${name.replaceFirstChar { it.uppercaseChar() }}C",
            "(I)C",
            null,
            null
        )

        mv.visitCode()

        mv.visitLdcInsn(coeff.last())
        for (i in coeff.size - 2 downTo 0) {
            mv.visitVarInsn(Opcodes.ILOAD, 0) // i
            mv.visitInsn(Opcodes.I2D)         // double(i)
            mv.visitInsn(Opcodes.DMUL)        // result * i
            mv.visitLdcInsn(coeff[i])         // coeff[i]
            mv.visitInsn(Opcodes.DADD)        // coeff[i] + i * result
        }

        mv.visitLdcInsn(0.5)
        mv.visitInsn(Opcodes.DADD)

        mv.visitInsn(Opcodes.D2I)
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitMaxs(4, 1)
        mv.visitEnd()
    }
}