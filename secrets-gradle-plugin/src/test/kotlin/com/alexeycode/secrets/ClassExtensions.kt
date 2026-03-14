package com.alexeycode.secrets

fun ByteArray.loadAsClass(): Class<*> {
    val clazz = object : ClassLoader() {
        fun define(): Class<*> {
            return defineClass(null, this@loadAsClass, 0, this@loadAsClass.size)
        }
    }.define()
    return clazz
}