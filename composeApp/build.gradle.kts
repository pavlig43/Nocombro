import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.libs

plugins {

    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.pavlig43.koin)
    alias(libs.plugins.pavlig43.decompose)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)

}

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

kotlin {
    // Desktop target
    jvm("desktop")

    // Configure source sets
    sourceSets {
        val commonMain by getting {
            dependencies {
                commonMainDependencies {
                    implementation(projects.rootnocombro)
                    implementation(libs.kotlinx.coroutines.core)
                }
            }
        }
    }
}

// Configure Desktop application
extensions.getByType<ComposeExtension>().extensions.configure<DesktopExtension> {
    application {
        this.mainClass = "ru.pavlig43.nocombro.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            this.packageName = "ru.pavlig43.nocombro"
            this.packageVersion = libs.versions.versionName.get()
        }
    }
}



