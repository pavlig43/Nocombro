package ru.pavlig43.convention.extension

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler


fun Project.commonMainDependencies(block: KotlinDependencyHandler.() -> Unit) {
    kotlinMultiplatformConfig {
        sourceSets.commonMain.dependencies(block)
    }
}

fun Project.commonTestDependencies(block: KotlinDependencyHandler.() -> Unit) {
    kotlinMultiplatformConfig {
        sourceSets.commonTest.dependencies(block)
    }
}

fun Project.androidMainDependencies(block: KotlinDependencyHandler.() -> Unit) {
    kotlinMultiplatformConfig {
        sourceSets.androidMain.dependencies(block)
    }
}


fun Project.desktopDependencies(block: KotlinDependencyHandler.() -> Unit) {
    kotlinMultiplatformConfig {
        sourceSets.getByName("desktopMain").dependencies(block)
    }
}
fun DependencyHandlerScope.detektPlugins(dependency: Provider<MinimalExternalModuleDependency>){
    add("detektPlugins",dependency)
}

//fun Project.iosMainDependencies(block: KotlinDependencyHandler.() -> Unit) {
//    kotlinMultiplatformConfig {
//        sourceSets.iosMain.dependencies(block)
//    }
//}






