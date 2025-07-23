package ru.pavlig43.convention.extension

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension


val Project.libs: LibrariesForLibs
    get() = the<LibrariesForLibs>()

internal val Project.projectJavaVersion: JavaVersion
    get() = JavaVersion.toVersion(libs.versions.java.get().toInt())

internal val Project.jvmTarget
    get() = JvmTarget.fromTarget(libs.versions.java.get())



internal fun Project.kotlinMultiplatformConfig(block: KotlinMultiplatformExtension.() -> Unit) {
    extensions.findByType<KotlinMultiplatformExtension>()
        ?.apply(block)
        ?: error("Kotlin multiplatform was not been added")
}





