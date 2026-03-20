package com.alexeycode.secrets

open class SecretsExtension {
    val secretsContainer = SecretsContainer()
    val flavorsSecrets = mutableMapOf<String, SecretsContainer>()

    /**
     * Secrets hardening method.
     *
     * [SecretsHardeningMethod.ZLIB] by default.
     */
    var method: SecretsHardeningMethod = SecretsHardeningMethod.ZLIB

    /**
     * Add secret [value] with [key].
     */
    fun key(name: String, value: () -> String) {
        secretsContainer.key(name, value)
    }

    /**
     * Add secrets for flavor with [name].
     */
    fun flavor(name: String, builder: SecretsContainer.() -> Unit) {
        val flavorContainer = flavorsSecrets.getOrPut(name) { SecretsContainer() }
        flavorContainer.builder()
    }

    class SecretsContainer {
        val secrets = mutableMapOf<String, String>()

        fun key(name: String, value: () -> String) {
            secrets[name] = value()
        }
    }
}