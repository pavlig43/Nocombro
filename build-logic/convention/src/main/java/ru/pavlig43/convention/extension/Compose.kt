package ru.pavlig43.convention.extension

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile




private val Project.compose
    get() = extensions.getByType<ComposeExtension>().dependencies



internal fun Project.configureComposeKmp() {

    dependencies{
        add("debugImplementation",compose.uiTooling)
    }


    androidMainDependencies  {
        implementation(compose.preview)
    }
    commonMainDependencies {
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.ui)
        implementation(compose.components.resources)
        implementation(compose.components.uiToolingPreview)
        implementation(compose.materialIconsExtended)
        implementation(libs.androidx.lifecycle.runtime.compose)
        implementation(compose.components.resources)
    }
    desktopDependencies  {
        implementation(compose.desktop.currentOs)
    }




    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            optIn.addAll(
                setOf(
                    "androidx.compose.ui.ExperimentalComposeUiApi",
                    "androidx.compose.ui.text.ExperimentalTextApi",
                    "androidx.compose.material.ExperimentalMaterialApi",
                    "androidx.compose.material3.ExperimentalMaterial3Api",
                    "androidx.compose.animation.ExperimentalAnimationApi",
                    "androidx.compose.foundation.ExperimentalFoundationApi",
                    "androidx.compose.foundation.layout.ExperimentalLayoutApi"
                )
            )
        }
    }
}

