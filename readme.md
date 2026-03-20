# Secrets

[![Build and tests][build-status-badge]][build-status-link]

[![GitHub License][license-badge]][license-link]
[![Gradle plugin version][gradle-plugin-badge]][gradle-plugin-link]

Protect API keys (or other secrets) from reverse engineering.

This plugin allows you to store secret keys (for example, API keys) 
in the resulting binary without worrying about static vulnerability 
analyzers.

## Usage
Gradle Kotlin DSL:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    id("com.alexeycode.secrets")
}

secrets {
    key("secret1") { "value1" } // do not store secrets in build files
    key("secret2") { "value2" } // use environment variables, or properties
}
```

Then secrets are available in the code by method call 
(`Secrets` class will be generated in your default package):
```kotlin
val secretValue = Secrets.getSecret1()
```
`Secrets` class bytecode does not contain a secret value literal. Instead, it 
contains some algorithm which will build the value in runtime.

### Secrets for product flavors
```kotlin
secrets {
    key("commonSecret") { "..." }
    flavor("production") { // will be applied only for specific flavor name
        key("flavorSecret") { "..." }
    }
    flavor("development") {
        key("flavorSecret") { "..." }
    }
}

android {
    ...
    flavorDimensions.add("env")
    productFlavors {
        create("production") {
            dimension = "env"
        }
        create("development") {
            dimension = "env"
        }
    }
}
```
In that case `Secrets.getFlavorSecret()` will return different result 
depends on a build flavor.

## Why is this a secure approach?

Okay, it is not 100% secure, of course. Deep static analysis
or runtime investigation of your binary will be able to find 
your secrets. However, automatic static analyzers will not 
find anything most probably.

Secrets are stored in the binary in an encoded format. In fact, 
the plugin does not use string literals at all - only algorithms 
that **reconstruct** secret values at runtime.

Moreover, as a developer you will not be able to see the secret 
values in the IDE when decompiling the Secrets class, because 
they are inserted into the bytecode at compile time.

## Motivation
Almost every Android application contains API keys. We use them for
analytics services, maps, simple REST APIs, and many different other things.
Usually we place them in the code like this:
```kotlin
const val API_KEY = "your api key"
```
or in resources like this:
```xml
<string name="api_key">your api key</string>
```
This way of storing keys is quite simple, but it also makes them
easy to extract with basic static analysis tools, since the keys
are embedded directly in the application binary. [Millions of secrets
leak every year because of that.](https://github.blog/security/application-security/next-evolution-github-advanced-security/)

I started thinking about how this could be made harder. How can we store
keys in a binary in a way that makes them difficult to extract?

As an experiment, I created this Gradle plugin.

[build-status-badge]: https://github.com/nikialeksey/secrets/actions/workflows/check.yml/badge.svg
[build-status-link]: https://github.com/nikialeksey/secrets/actions/workflows/check.yml
[license-badge]: https://img.shields.io/github/license/nikialeksey/secrets
[license-link]: https://github.com/nikialeksey/secrets/blob/main/LICENSE
[gradle-plugin-badge]: https://img.shields.io/gradle-plugin-portal/v/com.alexeycode.secrets
[gradle-plugin-link]: https://plugins.gradle.org/plugin/com.alexeycode.secrets