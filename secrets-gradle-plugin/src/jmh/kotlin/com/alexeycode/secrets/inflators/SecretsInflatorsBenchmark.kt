package com.alexeycode.secrets.inflators

import com.alexeycode.secrets.SecretsClassProvider
import com.alexeycode.secrets.loadAsClass
import com.alexeycode.secrets.transform
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

private const val CLASS_NAME = "com/example/Example"
private const val SECRET_SMALL_KEY = "smallKey"
private const val SECRET_SMALL_VALUE = "value"
private const val SECRET_SMALL_METHOD = "getSmallKey"
private const val SECRET_LONG_KEY = "longKey"
private const val SECRET_LONG_VALUE = "jkndgkjbfibuwiyf23we8ofihsdjvbihvuyAFIT#*IGUBRKSJKGOIOGIUEBGOR34TO"
private const val SECRET_LONG_METHOD = "getLongKey"
private val SECRETS = mapOf(
    SECRET_SMALL_KEY to SECRET_SMALL_VALUE,
    SECRET_LONG_KEY to SECRET_LONG_VALUE
)

private fun inflateNevilleSecretsClass(): Class<*> {
    val classBytes = SecretsClassProvider(CLASS_NAME, SECRETS.keys.toList()).get()
    return classBytes
        .transform { SecretsNevilleInflator(SECRETS, CLASS_NAME, it) }
        .loadAsClass()
}

private fun inflateZlibSecretsClass(): Class<*> {
    val classBytes = SecretsClassProvider(CLASS_NAME, SECRETS.keys.toList()).get()
    return classBytes
        .transform { SecretsZlibInflator(SECRETS, CLASS_NAME, it) }
        .loadAsClass()
}

private fun inflateSimpleSecretsClass(): Class<*> {
    val classBytes = SecretsClassProvider(CLASS_NAME, SECRETS.keys.toList()).get()
    return classBytes
        .transform { SecretsSimpleInflator(SECRETS, it) }
        .loadAsClass()
}

open class SecretsInflatorsBenchmark {

    @State(Scope.Thread)
    open class SecretsNevilleObjectState {
        val secretsInflatorClass = inflateNevilleSecretsClass()
        val secretsGetSmallSecretMethod = secretsInflatorClass.getDeclaredMethod(SECRET_SMALL_METHOD)
        val secretsGetLongSecretMethod = secretsInflatorClass.getDeclaredMethod(SECRET_LONG_METHOD)
    }

    @State(Scope.Thread)
    open class SecretsZipObjectState {
        val secretsInflatorClass = inflateZlibSecretsClass()
        val secretsGetSmallSecretMethod = secretsInflatorClass.getDeclaredMethod(SECRET_SMALL_METHOD)
        val secretsGetLongSecretMethod = secretsInflatorClass.getDeclaredMethod(SECRET_LONG_METHOD)
    }

    @State(Scope.Thread)
    open class SecretsSimpleObjectState {
        val secretsInflatorClass = inflateSimpleSecretsClass()
        val secretsGetSmallSecretMethod = secretsInflatorClass.getDeclaredMethod(SECRET_SMALL_METHOD)
        val secretsGetLongSecretMethod = secretsInflatorClass.getDeclaredMethod(SECRET_LONG_METHOD)
    }

    @Benchmark
    fun getSmallSecretNeville(state: SecretsNevilleObjectState, blackhole: Blackhole) {
        val result = state.secretsGetSmallSecretMethod.invoke(state.secretsInflatorClass)
        blackhole.consume(result)
    }

    @Benchmark
    fun getSmallSecretZip(state: SecretsZipObjectState, blackhole: Blackhole) {
        val result = state.secretsGetSmallSecretMethod.invoke(state.secretsInflatorClass)
        blackhole.consume(result)
    }

    @Benchmark
    fun getSmallSecretSimple(state: SecretsSimpleObjectState, blackhole: Blackhole) {
        val result = state.secretsGetSmallSecretMethod.invoke(state.secretsInflatorClass)
        blackhole.consume(result)
    }

    @Benchmark
    fun getLongSecretNeville(state: SecretsNevilleObjectState, blackhole: Blackhole) {
        val result = state.secretsGetLongSecretMethod.invoke(state.secretsInflatorClass)
        blackhole.consume(result)
    }

    @Benchmark
    fun getLongSecretZip(state: SecretsZipObjectState, blackhole: Blackhole) {
        val result = state.secretsGetLongSecretMethod.invoke(state.secretsInflatorClass)
        blackhole.consume(result)
    }

    @Benchmark
    fun getLongSecretSimple(state: SecretsSimpleObjectState, blackhole: Blackhole) {
        val result = state.secretsGetLongSecretMethod.invoke(state.secretsInflatorClass)
        blackhole.consume(result)
    }
}