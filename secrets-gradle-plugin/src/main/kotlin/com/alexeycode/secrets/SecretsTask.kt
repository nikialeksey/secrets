package com.alexeycode.secrets

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

abstract class SecretsTask : DefaultTask() {

    @get:Input
    abstract val keys: ListProperty<String>

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @TaskAction
    fun generate() {
        val jarFile = outputJar.get().asFile
        jarFile.parentFile.mkdirs()
        JarOutputStream(FileOutputStream(jarFile)).use { jos ->
            val entry = JarEntry("com/alexeycode/secrets/Secrets.class")
            jos.putNextEntry(entry)
            jos.write(generateByteCode())
            jos.closeEntry()
        }
    }

    private fun generateByteCode(): ByteArray {
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)

        cw.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
            "com/alexeycode/secrets/Secrets",
            null,
            "java/lang/Object",
            null
        )

        for (key in keys.get()) {
            cw.visitField(
                Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
                key,
                "Ljava/lang/String;",
                null,  // signature
                null   // no value assigned => null
            ).visitEnd()
        }

        // default constructor
        val mv: MethodVisitor = cw.visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        )

        mv.visitCode()
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/Object",
            "<init>",
            "()V",
            false
        )
        mv.visitInsn(Opcodes.RETURN)
        mv.visitMaxs(1, 1)
        mv.visitEnd()

        cw.visitEnd()
        return cw.toByteArray()
    }
}
