package ru.pavlig43.convention.extension

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget


internal fun Project.configTargetWithoutAndroid(){
    kotlinMultiplatformConfig {
        jvm("desktop")
        //ios
        targets
            .filterIsInstance<KotlinNativeTarget>()
            .forEach { nativeTarget ->
                nativeTarget.binaries.framework {
                    baseName = project.name.replace("-", "")
                    isStatic = true
                }
            }

    }
}