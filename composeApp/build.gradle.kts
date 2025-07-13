import ru.pavlig43.convention.extension.composeDesktopApplication
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import ru.pavlig43.convention.extension.implementation
import ru.pavlig43.convention.extension.libs

plugins {

    alias(libs.plugins.pavlig43.application)
    alias(libs.plugins.pavlig43.kmp)

    alias(libs.plugins.pavlig43.compose)
    alias(libs.plugins.pavlig43.coroutines)
    alias(libs.plugins.pavlig43.koin)
    alias(libs.plugins.pavlig43.decompose)
    alias(libs.plugins.composeHotReload)



}
composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

kotlin {


    sourceSets {


        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)

        }
        commonMain.dependencies {

            implementation(projects.theme)
            implementation(projects.corekoin)
            implementation(projects.database)
            implementation(libs.datastore)
            implementation(libs.datastore.preferences)
            implementation(projects.datastore)
            implementation(projects.rootnocombro)
            implementation(libs.filekit.dialogs)

        }


    }
}
android{
    namespace = "ru.pavlig43.nocombro"
    defaultConfig{
        applicationId = "ru.pavlig43.nocombro"
    }

}
dependencies{
    debugImplementation(libs.leakcanary.android)
}
composeDesktopApplication(
    mainClass = "ru.pavlig43.nocombro.MainKt",
    packageName = "ru.pavlig43.nocombro"
)


