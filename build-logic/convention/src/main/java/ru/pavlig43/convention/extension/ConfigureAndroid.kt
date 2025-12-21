package ru.pavlig43.convention.extension

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project


internal fun Project.configureAndroid(
    commonExtension: CommonExtension<*,*,*,*,*,*>
){
    commonExtension.apply {
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
