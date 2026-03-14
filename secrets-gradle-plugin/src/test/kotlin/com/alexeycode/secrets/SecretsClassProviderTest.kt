package com.alexeycode.secrets

import org.junit.Assert
import org.junit.Test

class SecretsClassProviderTest {
    @Test
    fun checkClassContainsOneGetterForOneKey() {
        val classBytes = SecretsClassProvider("com/example/Example", listOf("key")).get()
        val clazz = classBytes.loadAsClass()

        val method = clazz.getDeclaredMethod("getKey")
        Assert.assertNotNull(method)
    }

    @Test
    fun checkClassContainsTwoGettersForTwoKeys() {
        val classBytes = SecretsClassProvider("com/example/Example", listOf("key1", "key2")).get()
        val clazz = classBytes.loadAsClass()

        val getKey1Method = clazz.getDeclaredMethod("getKey1")
        Assert.assertNotNull(getKey1Method)
        val getKey2Method = clazz.getDeclaredMethod("getKey2")
        Assert.assertNotNull(getKey2Method)
    }
}