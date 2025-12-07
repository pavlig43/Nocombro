package ru.pavlig43.convention.extension

import com.android.build.api.dsl.AndroidResources
import com.android.build.api.dsl.BuildFeatures
import com.android.build.api.dsl.BuildType
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DefaultConfig
import com.android.build.api.dsl.Installation
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

private typealias AndroidExtensions = CommonExtension<
        out BuildFeatures,
        out BuildType,
        out DefaultConfig,
        out ProductFlavor,
        out AndroidResources,
        out Installation>

internal val Project.androidExtension: AndroidExtensions
    get() = extensions.findByType(BaseAppModuleExtension::class)
        ?: extensions.findByType(LibraryExtension::class)
        ?: error(
            "\"Project.androidExtension\" value may be called only from android application" +
                    " or android library gradle script"
        )


internal fun Project.kotlinAndroidTarget(block: KotlinAndroidTarget.() -> Unit) {
    kotlinMultiplatformConfig {
        androidTarget(block)
    }
}
internal fun Project.androidConfig(block: AndroidExtensions.() -> Unit): Unit = block(androidExtension)

internal fun Project.configureAndroid(
) {

    androidExtension.apply {
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        lint {
            checkDependencies = true
        }
        defaultConfig {
            minSdk = libs.versions.android.minSdk.get().toInt()
        }
        compileOptions{
            sourceCompatibility = projectJavaVersion
            targetCompatibility = projectJavaVersion
        }

    }

}
