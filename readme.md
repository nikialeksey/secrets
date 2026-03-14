# Secrets

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
    key("secret1") { "value1" }
    key("secret2") { "value2" }
}
```

Then secrets are available in the code by 
reference:
```kotlin
com.alexeycode.secrets.Secrets.secret1
```

## Motivation
Almost every Android application contains API keys. We use them for 
analytics services, maps, simple REST APIs, and many different other things. 
Usually we place them in the code like this:
```kotlin
const val API_KEY = "your api key"
```
or in resources like this:
```xml
<string name"api_key">your api key</string>
```
This way of storing keys is quite simple, but it also makes them 
easy to extract with basic static analysis tools, since the keys 
are embedded directly in the application binary.

I started thinking about how this could be made harder. How can we store 
keys in a binary in a way that makes them difficult to extract?

As an experiment, I created this Gradle plugin.
