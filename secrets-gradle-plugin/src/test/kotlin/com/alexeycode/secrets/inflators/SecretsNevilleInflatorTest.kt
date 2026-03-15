package com.alexeycode.secrets.inflators

import com.alexeycode.secrets.SecretsClassProvider
import com.alexeycode.secrets.loadAsClass
import com.alexeycode.secrets.transform
import org.junit.Assert
import org.junit.Test

class SecretsNevilleInflatorTest {
    @Test
    fun inflateSimpleSecretAndReadIt() {
        val className = "com/example/Example"
        val secrets = mapOf("key" to "value")
        val classBytes = SecretsClassProvider(className, secrets.keys.toList()).get()
        val transformed = classBytes
            .transform { SecretsNevilleInflator(secrets, className, it) }
            .loadAsClass()

        val method = transformed.getDeclaredMethod("getKey")
        Assert.assertNotNull(method)

        val result = method.invoke(transformed)
        Assert.assertEquals("value", result)
    }

    @Test
    fun inflateLongSecretAndReadIt() {
        val className = "com/example/Example"
        val secrets = mapOf("key" to "hello, secret prod")
        val classBytes = SecretsClassProvider(className, secrets.keys.toList()).get()
        val transformed = classBytes
            .transform { SecretsNevilleInflator(secrets, className, it) }
            .loadAsClass()

        val method = transformed.getDeclaredMethod("getKey")
        Assert.assertNotNull(method)

        val result = method.invoke(transformed)
        Assert.assertEquals("hello, secret prod", result)
    }

    @Test
    fun inflateVeryLongSecretAndReadIt() {
        val secret = "sk_live_Q9f7L2xKp8Vt4Yw6Zr1N5bC3D0eA7HjM2uP6sX9Tq4R"
        val className = "com/example/Example"
        val secrets = mapOf("key" to secret)
        val classBytes = SecretsClassProvider(className, secrets.keys.toList()).get()
        val transformed = classBytes
            .transform { SecretsNevilleInflator(secrets, className, it) }
            .loadAsClass()

        val method = transformed.getDeclaredMethod("getKey")
        Assert.assertNotNull(method)

        val result = method.invoke(transformed)
        Assert.assertEquals(secret, result)
    }
}