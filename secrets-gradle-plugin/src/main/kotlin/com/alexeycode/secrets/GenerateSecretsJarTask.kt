package com.alexeycode.secrets

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

@CacheableTask
abstract class GenerateSecretsJarTask : DefaultTask() {

    @get:Input
    abstract val keys: ListProperty<String>

    @get:Input
    abstract val className: Property<String>

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @TaskAction
    fun generate() {
        val jarFile = outputJar.get().asFile
        jarFile.parentFile.mkdirs()
        val className = className.get()
        val keys = keys.get()
        JarOutputStream(FileOutputStream(jarFile)).use { jos ->
            val entry = JarEntry("$className.class")
            val secretsClassProvider = SecretsClassProvider(className, keys)
            jos.putNextEntry(entry)
            jos.write(secretsClassProvider.get())
            jos.closeEntry()
        }
    }
}
