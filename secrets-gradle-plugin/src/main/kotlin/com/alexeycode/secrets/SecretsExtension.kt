package com.alexeycode.secrets

open class SecretsExtension {
    val secrets = mutableMapOf<String, String>()

    fun key(name: String, value: () -> String) {
        secrets[name] = value()
    }


}