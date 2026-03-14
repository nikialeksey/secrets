plugins {
    id("java-gradle-plugin")
    kotlin("jvm") version "2.3.10"
}

group = "com.alexeycode"
version = "0.0.1"

dependencies {
    implementation("com.android.tools.build:gradle-api:9.1.0")
    implementation("org.ow2.asm:asm:9.9.1")
    implementation("org.apache.commons:commons-math3:3.6.1")
}

gradlePlugin {
    plugins {
        create("apikey") {
            id = "com.alexeycode.secrets"
            displayName = "ApiKey"
            implementationClass = "com.alexeycode.secrets.SecretsPlugin"
        }
    }
}