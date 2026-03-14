plugins {
    id("java-gradle-plugin")
    kotlin("jvm") version "2.3.10"
    id("com.gradle.plugin-publish") version "2.1.0"
}

group = "com.alexeycode.secrets"
version = "0.0.1"

dependencies {
    implementation("com.android.tools.build:gradle-api:9.1.0")
    implementation("org.ow2.asm:asm:9.9.1")
    implementation("org.apache.commons:commons-math3:3.6.1")
}

gradlePlugin {
    website = "https://github.com/nikialeksey/secrets"
    vcsUrl = "https://github.com/nikialeksey/secrets"

    plugins {
        create("secrets") {
            id = "com.alexeycode.secrets"
            displayName = "Secrets"
            description = "Plugin to store your secrets in a secure way in binary."
            tags = listOf("android", "secrets", "api", "keys")
            implementationClass = "com.alexeycode.secrets.SecretsPlugin"
        }
    }
}