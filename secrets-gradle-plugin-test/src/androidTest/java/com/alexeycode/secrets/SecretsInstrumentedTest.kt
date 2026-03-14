package com.alexeycode.secrets

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class SecretsInstrumentedTest {
    @Test
    fun secretsAreAvailableInRuntime() {
        Assert.assertEquals("hello, secret 1", Secrets.getSecret1())
    }
}