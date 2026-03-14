package com.alexeycode.secrets

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

fun ByteArray.loadAsClass(): Class<*> {
    val clazz = object : ClassLoader() {
        fun define(): Class<*> {
            return defineClass(null, this@loadAsClass, 0, this@loadAsClass.size)
        }
    }.define()
    return clazz
}

fun ByteArray.transform(visitor: (ClassVisitor) -> ClassVisitor): ByteArray {
    val reader = ClassReader(this)
    val writer = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

    reader.accept(visitor(writer), 0)

    return writer.toByteArray()
}