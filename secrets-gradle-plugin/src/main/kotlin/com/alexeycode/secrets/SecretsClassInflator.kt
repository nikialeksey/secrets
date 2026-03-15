package com.alexeycode.secrets

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.math.BigDecimal
import java.math.MathContext

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
                        val cMethod = name + "C"
                        val len = value.length

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
            val x = Array<BigDecimal>(valueLength + 1) { BigDecimal.ZERO }
            val y = Array<BigDecimal>(valueLength + 1) { BigDecimal.ZERO }
            for (i in 0 until valueLength) {
                x[i] = i.toBigDecimal()
                y[i] = value[i].code.toBigDecimal()
            }
            x[valueLength] = valueLength.toBigDecimal()
            y[valueLength] = BigDecimal.ZERO

            val mc = MathContext(50)
            val coeff = computeCoefficients(x, y, mc)

            generateCoeffMethod(cv, key, coeff, mc.precision)
        }
        super.visitEnd()
    }

    // from NevilleInterpolator (Apache Commons math)
    fun computeCoefficients(
        x: Array<BigDecimal>,
        y: Array<BigDecimal>,
        mc: MathContext
    ): Array<BigDecimal> {
        val n = x.size
        val coefficients = Array<BigDecimal>(n) { BigDecimal.ZERO }
        val c = Array<BigDecimal>(n + 1) { BigDecimal.ZERO }
        c[0] = BigDecimal.ONE

        for (i in 0..<n) {
            for (j in i downTo 1) {
                c[j] = c[j - 1].subtract(c[j].multiply(x[i], mc), mc)
            }
            c[0] = c[0].multiply(x[i].negate(), mc)
            c[i + 1] = BigDecimal.ONE
        }

        val tc = Array<BigDecimal>(n) { BigDecimal.ZERO }

        for (i in 0..<n) {
            var d = BigDecimal.ONE
            for (j in 0..<n) {
                if (i != j) {
                    d = d.multiply(x[i].subtract(x[j], mc), mc)
                }
            }

            val t = y[i].divide(d, mc)

            tc[n - 1] = c[n]
            coefficients[n - 1] = coefficients[n - 1].add(t.multiply(tc[n - 1], mc), mc)

            for (j in n - 2 downTo 0) {
                tc[j] = c[j + 1].add(tc[j + 1].multiply(x[i], mc), mc)
                coefficients[j] = coefficients[j].add(t.multiply(tc[j], mc), mc)
            }
        }

        return coefficients
    }

    private fun generateCoeffMethod(
        cv: ClassVisitor,
        name: String,
        coeff: Array<BigDecimal>,
        precision: Int
    ) {
        val mv = cv.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "get${name.replaceFirstChar { it.uppercaseChar() }}C",
            "(I)C",
            null,
            null
        )

        mv.visitCode()

        // MathContext mc = new MathContext(precision)
        mv.visitTypeInsn(Opcodes.NEW, "java/math/MathContext")
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn(precision)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/math/MathContext",
            "<init>",
            "(I)V",
            false
        )
        mv.visitVarInsn(Opcodes.ASTORE, 1)

        // BigDecimal result = new BigDecimal(coeff.last())
        mv.visitTypeInsn(Opcodes.NEW, "java/math/BigDecimal")
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn(coeff.last().toString())
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/math/BigDecimal",
            "<init>",
            "(Ljava/lang/String;)V",
            false
        )
        mv.visitVarInsn(Opcodes.ASTORE, 2)

        for (i in coeff.size - 2 downTo 0) {

            // result.multiply(BigDecimal.valueOf(i), mc)

            mv.visitVarInsn(Opcodes.ALOAD, 2)

            mv.visitVarInsn(Opcodes.ILOAD, 0)
            mv.visitInsn(Opcodes.I2L)

            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/math/BigDecimal",
                "valueOf",
                "(J)Ljava/math/BigDecimal;",
                false
            )

            mv.visitVarInsn(Opcodes.ALOAD, 1)

            mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/math/BigDecimal",
                "multiply",
                "(Ljava/math/BigDecimal;Ljava/math/MathContext;)Ljava/math/BigDecimal;",
                false
            )

            // + coeff[i]

            mv.visitTypeInsn(Opcodes.NEW, "java/math/BigDecimal")
            mv.visitInsn(Opcodes.DUP)
            mv.visitLdcInsn(coeff[i].toString())
            mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/math/BigDecimal",
                "<init>",
                "(Ljava/lang/String;)V",
                false
            )

            mv.visitVarInsn(Opcodes.ALOAD, 1)

            mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/math/BigDecimal",
                "add",
                "(Ljava/math/BigDecimal;Ljava/math/MathContext;)Ljava/math/BigDecimal;",
                false
            )

            mv.visitVarInsn(Opcodes.ASTORE, 2)
        }

        // result = result + 0.5

        mv.visitVarInsn(Opcodes.ALOAD, 2)

        mv.visitTypeInsn(Opcodes.NEW, "java/math/BigDecimal")
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn("0.5")
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/math/BigDecimal",
            "<init>",
            "(Ljava/lang/String;)V",
            false
        )

        mv.visitVarInsn(Opcodes.ALOAD, 1)

        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/math/BigDecimal",
            "add",
            "(Ljava/math/BigDecimal;Ljava/math/MathContext;)Ljava/math/BigDecimal;",
            false
        )

        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/math/BigDecimal",
            "intValue",
            "()I",
            false
        )

        mv.visitInsn(Opcodes.IRETURN)

        mv.visitMaxs(5, 3)
        mv.visitEnd()
    }

}