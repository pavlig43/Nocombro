package ru.pavlig43.convention.extension

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.implementation(dependency: Provider<MinimalExternalModuleDependency>){
    add("implementation",dependency)
}
fun DependencyHandlerScope.debugImplementation(dependency: Provider<Dependency>){
    add("debugImplementation",dependency)
}
fun DependencyHandlerScope.detektPlugins(dependency: Provider<MinimalExternalModuleDependency>){
    add("detektPlugins",dependency)
}

