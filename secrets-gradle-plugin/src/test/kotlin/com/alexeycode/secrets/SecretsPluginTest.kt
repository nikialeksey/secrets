package com.alexeycode.secrets

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipFile


class SecretsPluginTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun assembleDebugTest() {
        val projectRoot = temporaryFolder.newFolder()

        File(projectRoot, "settings.gradle").writeText("""
            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
            }        
        """.trimIndent())
        File(projectRoot, "build.gradle").writeText("""
            plugins {
                id 'com.android.application'
                id 'com.alexeycode.secrets'
            }
    
            android {
                namespace "com.example.test"
                compileSdk {
                    version = release(36) {
                        minorApiLevel = 1
                    }
                }
            }
    
            secrets {
                key("key") { "value" }
            }
        """.trimIndent())

        val manifest = File(projectRoot, "src/main/AndroidManifest.xml")
        manifest.parentFile.mkdirs()
        manifest.writeText("""
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.example.test">
                <application android:label="Test"/>
            </manifest>
        """.trimIndent())

        val result = GradleRunner.create()
            .withGradleVersion("9.4.0")
            .withProjectDir(projectRoot)
            .withArguments("assembleDebug")
            .withPluginClasspath()
            .build()

        Assert.assertTrue(result.output.contains("BUILD SUCCESSFUL"))

        val outputDir = File(projectRoot, "build/outputs/apk/debug/")
        val apkFile = File(outputDir, outputDir.list().first { it.contains(".apk") })

        val apk = ZipFile(apkFile)
        val dexFile = apk.getInputStream(apk.getEntry("classes.dex")).use { raw ->
            BufferedInputStream(raw).use { dexStream ->
                DexBackedDexFile.fromInputStream(Opcodes.forApi(36), dexStream)
            }
        }

        val secretsClass = dexFile.classes.find { it.type == "Lcom/example/test/Secrets;" }
        Assert.assertNotNull(secretsClass)
        val getSecretMethod = secretsClass?.directMethods?.find { it.name == "getKey" }
        Assert.assertNotNull(getSecretMethod)
    }
}