package com.alexeycode.secrets.inflators

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

class SecretsZlibInflator(
    private val secrets: Map<String, String>,
    private val className: String,
    nextClassVisitor: ClassVisitor
) : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {

    override fun visitEnd() {
        for (key in secrets.keys) {
            cv.visitField(
                Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL,
                key,
                "[B",
                null,
                null
            ).visitEnd()
        }

        val mv = cv.visitMethod(
            Opcodes.ACC_STATIC,
            "<clinit>",
            "()V",
            null,
            null
        )

        mv.visitCode()

        for ((key, value) in secrets) {
            val bytes = deflate(value)

            pushByteArray(mv, bytes)

            mv.visitFieldInsn(
                Opcodes.PUTSTATIC,
                className,
                key,
                "[B"
            )
        }

        mv.visitInsn(Opcodes.RETURN)
        mv.visitMaxs(4, 0)
        mv.visitEnd()

        generateInflate()

        super.visitEnd()
    }

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

                    override fun visitInsn(opcode: Int) {}
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

                        mv.visitFieldInsn(
                            Opcodes.GETSTATIC,
                            className,
                            key,
                            "[B"
                        )

                        mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            className,
                            "inflate",
                            "([B)Ljava/lang/String;",
                            false
                        )

                        mv.visitInsn(Opcodes.ARETURN)
                        mv.visitMaxs(2, 0)
                        super.visitEnd()
                    }
                }
            }
        }

        return mv
    }

    private fun deflate(input: String): ByteArray {
        val data = input.toByteArray(Charsets.UTF_8)
        val deflater = Deflater()
        deflater.setInput(data)
        deflater.finish()

        val bos = ByteArrayOutputStream()
        val buffer = ByteArray(256)

        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            bos.write(buffer, 0, count)
        }

        return bos.toByteArray()
    }

    private fun generateInflate() {
        val mv = cv.visitMethod(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
            "inflate",
            "([B)Ljava/lang/String;",
            null,
            null
        )

        mv.visitCode()

        // Inflater inflater = new Inflater();
        mv.visitTypeInsn(Opcodes.NEW, "java/util/zip/Inflater")
        mv.visitInsn(Opcodes.DUP)
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/zip/Inflater", "<init>", "()V", false)
        mv.visitVarInsn(Opcodes.ASTORE, 1)

        val tryStart = Label()
        val tryEnd = Label()
        val catchLabel = Label()

        mv.visitLabel(tryStart)

        // inflater.setInput(input)
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/zip/Inflater", "setInput", "([B)V", false)

        // ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        mv.visitTypeInsn(Opcodes.NEW, "java/io/ByteArrayOutputStream")
        mv.visitInsn(Opcodes.DUP)
        mv.visitIntInsn(Opcodes.SIPUSH, 1024)
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/ByteArrayOutputStream", "<init>", "(I)V", false)
        mv.visitVarInsn(Opcodes.ASTORE, 2)

        // byte[] buffer = new byte[4096];
        mv.visitIntInsn(Opcodes.SIPUSH, 1024)
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE)
        mv.visitVarInsn(Opcodes.ASTORE, 3)

        val loop = Label()
        val end = Label()
        val write = Label()

        mv.visitLabel(loop)

        // if (inflater.finished()) break;
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/zip/Inflater", "finished", "()Z", false)
        mv.visitJumpInsn(Opcodes.IFNE, end)

        // int count = inflater.inflate(buffer);
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitVarInsn(Opcodes.ALOAD, 3)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/zip/Inflater", "inflate", "([B)I", false)
        mv.visitVarInsn(Opcodes.ISTORE, 4)

        // if (count != 0) -> write
        mv.visitVarInsn(Opcodes.ILOAD, 4)
        mv.visitJumpInsn(Opcodes.IFNE, write)

        // if (inflater.needsInput()) break;
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/zip/Inflater", "needsInput", "()Z", false)
        mv.visitJumpInsn(Opcodes.IFNE, end)

        // else error, throw an exception
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException")
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn("Bad compressed data")
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/RuntimeException",
            "<init>",
            "(Ljava/lang/String;)V",
            false
        )
        mv.visitInsn(Opcodes.ATHROW)

        // write:
        mv.visitLabel(write)

        // out.write(buffer, 0, count)
        mv.visitVarInsn(Opcodes.ALOAD, 2)
        mv.visitVarInsn(Opcodes.ALOAD, 3)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitVarInsn(Opcodes.ILOAD, 4)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/io/ByteArrayOutputStream",
            "write",
            "([BII)V",
            false
        )

        mv.visitJumpInsn(Opcodes.GOTO, loop)

        mv.visitLabel(end)

        // return new String(out.toByteArray(), UTF_8)
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/String")
        mv.visitInsn(Opcodes.DUP)
        mv.visitVarInsn(Opcodes.ALOAD, 2)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/io/ByteArrayOutputStream",
            "toByteArray",
            "()[B",
            false
        )
        mv.visitFieldInsn(
            Opcodes.GETSTATIC,
            "java/nio/charset/StandardCharsets",
            "UTF_8",
            "Ljava/nio/charset/Charset;"
        )
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/String",
            "<init>",
            "([BLjava/nio/charset/Charset;)V",
            false
        )

        mv.visitLabel(tryEnd)

        // inflater.end() перед return
        mv.visitVarInsn(Opcodes.ASTORE, 4)
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/zip/Inflater", "end", "()V", false)
        mv.visitVarInsn(Opcodes.ALOAD, 4)
        mv.visitInsn(Opcodes.ARETURN)

        // catch(Throwable t)
        mv.visitLabel(catchLabel)
        mv.visitVarInsn(Opcodes.ASTORE, 4)

        // inflater.end()
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/zip/Inflater", "end", "()V", false)

        // throw t
        mv.visitVarInsn(Opcodes.ALOAD, 4)
        mv.visitInsn(Opcodes.ATHROW)

        mv.visitTryCatchBlock(tryStart, tryEnd, catchLabel, null)

        mv.visitMaxs(5, 5)
        mv.visitEnd()
    }

    private fun pushByteArray(mv: MethodVisitor, data: ByteArray) {
        mv.visitIntInsn(Opcodes.SIPUSH, data.size)
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE)

        for (i in data.indices) {
            mv.visitInsn(Opcodes.DUP)
            mv.visitIntInsn(Opcodes.SIPUSH, i)
            mv.visitIntInsn(Opcodes.BIPUSH, data[i].toInt())
            mv.visitInsn(Opcodes.BASTORE)
        }
    }
}