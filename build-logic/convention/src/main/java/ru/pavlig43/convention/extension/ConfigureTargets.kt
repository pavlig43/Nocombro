package ru.pavlig43.convention.extension

import org.gradle.api.Project

fun Project.configureTargets(
    excludeTargets: Array<Target> = arrayOf()
) {
    val filterTargets = Target.values().filter { it !in excludeTargets }

    kotlinMultiplatformConfig {

        filterTargets.forEach { target ->

            when (target) {
                Target.Android -> androidTarget()
//                Target.Jvm -> jvm()
//                Target.IosX64 -> iosX64()
//                Target.IosArm64 -> iosArm64()
//                Target.IosSimulatorArm64 -> iosSimulatorArm64()
//                Target.WasmJs -> wasmTarget()
                Target.Desktop-> jvm("desktop")

            }

        }

    }
}


enum class Target {
    Android,
    Desktop,

//    Jvm,
//    IosX64,
//    IosArm64,
//    IosSimulatorArm64,
//    WasmJs
}

