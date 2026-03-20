package com.alexeycode.secrets.inflators

import com.alexeycode.secrets.SecretsClassProvider
import com.alexeycode.secrets.SecretsHardeningMethod
import com.alexeycode.secrets.loadAsClass
import com.alexeycode.secrets.transform
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.objectweb.asm.ClassVisitor

@RunWith(Parameterized::class)
class SecretsInflatorsTest(
    private val method: SecretsHardeningMethod,
    private val inflatorFactory: (secrets: Map<String, String>, className: String, nextClassVisitor: ClassVisitor) -> ClassVisitor
) {
    @Test
    fun inflateSimpleSecretAndReadIt() {
        val className = "com/example/Example"
        val secretValue = "value"
        val secrets = mapOf("key" to secretValue)
        val classBytes = SecretsClassProvider(className, secrets.keys.toList()).get()
        val transformed = classBytes
            .transform { inflatorFactory(secrets, className, it) }
            .loadAsClass()

        val method = transformed.getDeclaredMethod("getKey")
        Assert.assertNotNull(method)

        val result = method.invoke(transformed)
        Assert.assertEquals(secretValue, result)
    }

    @Test
    fun inflateLongSecretAndReadIt() {
        val className = "com/example/Example"
        val secrets = mapOf("key" to "hello, secret prod")
        val classBytes = SecretsClassProvider(className, secrets.keys.toList()).get()
        val transformed = classBytes
            .transform { inflatorFactory(secrets, className, it) }
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
            .transform { inflatorFactory(secrets, className, it) }
            .loadAsClass()

        val method = transformed.getDeclaredMethod("getKey")
        Assert.assertNotNull(method)

        val result = method.invoke(transformed)
        Assert.assertEquals(secret, result)
    }

    companion object {
        @Parameterized.Parameters(name = "{index}: inflator method: {0}")
        @JvmStatic
        fun data(): List<Array<Any>> {
            return listOf(
                arrayOf(
                    SecretsHardeningMethod.NONE,
                    { secrets: Map<String, String>, className: String, nextClassVisitor: ClassVisitor ->
                        SecretsSimpleInflator(secrets, nextClassVisitor)
                    }
                ),
                arrayOf(
                    SecretsHardeningMethod.NEVILLE,
                    { secrets: Map<String, String>, className: String, nextClassVisitor: ClassVisitor ->
                        SecretsNevilleInflator(secrets, className, nextClassVisitor)
                    },
                ),
                arrayOf(
                    SecretsHardeningMethod.ZLIB,
                    { secrets: Map<String, String>, className: String, nextClassVisitor: ClassVisitor ->
                        SecretsZlibInflator(secrets, className, nextClassVisitor)
                    }
                )
            )
        }
    }
}