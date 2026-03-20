package com.alexeycode.secrets

enum class SecretsHardeningMethod {
    /**
     * Without any hardening.
     */
    NONE,

    /**
     * Heavy hardening based [Neville polynomial](https://en.wikipedia.org/wiki/Neville%27s_algorithm)
     */
    NEVILLE,

    /**
     * Hardening based on Zlib.
     */
    ZLIB,
}