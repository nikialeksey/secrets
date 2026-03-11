package com.alexeycode.secrets

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.extensions.stdlib.capitalized
import javax.inject.Inject

class SecretsPlugin(
    private val extensionName: String,
    private val taskPrefix: String,
    private val taskSuffix: String
) : Plugin<Project> {

    @Inject
    constructor() : this(
        extensionName = "secrets",
        taskPrefix = "generate",
        taskSuffix = "secrets"
    )

    override fun apply(target: Project) {
        target.extensions.create(
            extensionName,
            SecretsExtension::class.java,
        )

        val androidComponents = target.extensions.findByType(AndroidComponentsExtension::class.java)
        if (androidComponents != null) {
            configureAndroid(target, androidComponents)
        } else {
            // TODO configureJava()
        }
    }

    private fun configureAndroid(
        target: Project,
        androidComponents: AndroidComponentsExtension<*, *, *>
    ) {
        androidComponents.onVariants { variant ->
            val secretsExtension = target.extensions
                .getByType(SecretsExtension::class.java)

            val generateSecretsTask = target.tasks.register(
                "$taskPrefix${variant.name.capitalized()}${taskSuffix.capitalized()}",
                SecretsTask::class.java
            ) { task ->
                task.keys.set(secretsExtension.secrets.keys.toList())
                task.outputJar.set(
                    target.layout.buildDirectory.file("generated/secrets/${variant.name}/secrets.jar")
                )
            }

            target.dependencies.add(
                "${variant.name}Implementation",
                target.files(generateSecretsTask.map { it.outputJar })
            )

            variant.instrumentation.transformClassesWith(
                SecretsClassVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) { params ->
                params.secrets.set(secretsExtension.secrets)
            }

            variant.instrumentation.setAsmFramesComputationMode(
                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
            )
        }
    }
}