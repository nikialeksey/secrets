package com.alexeycode.secrets

import org.junit.Assert
import org.junit.Test

class SecretsUnitTest {
    @Test
    fun `secrets are not available in unit tests`() {
        Assert.assertEquals(null, Secrets.getSecret1())
    }
}