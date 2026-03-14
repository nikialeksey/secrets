package com.alexeycode.secrets

import org.junit.Assert
import org.junit.Test

class SecretsClassInflatorTest {
    @Test
    fun asdasd() {
        val className = "com/example/Example"
        val secrets = mapOf("key" to "value")
        val classBytes = SecretsClassProvider(className, secrets.keys.toList()).get()
        val transformed = classBytes
            .transform { SecretsClassInflator(secrets, className, it) }
            .loadAsClass()

        val method = transformed.getDeclaredMethod("getKey")
        Assert.assertNotNull(method)

        val result = method.invoke(transformed)
        Assert.assertEquals("value", result)
    }
}