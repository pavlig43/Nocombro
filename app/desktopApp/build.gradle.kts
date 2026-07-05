import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import ru.pavlig43.convention.extension.desktopDependencies
import ru.pavlig43.convention.extension.libs

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin{
    jvm("desktop")
    desktopDependencies {
        implementation(libs.filekit.dialogs.compose)
        implementation(libs.koin.core)
        implementation(libs.decompose)
        implementation(libs.decompose.compose)
        implementation(libs.compose.ui.backhandler.desktop)
        implementation(libs.androidx.navigationevent.desktop)
        implementation(libs.kermit)
        implementation(projects.rootnocombro)
        implementation(compose.desktop.currentOs)
        implementation(projects.coreui)
    }
}

extensions.getByType<ComposeExtension>().extensions.configure<DesktopExtension> {
    application {
        this.mainClass = "ru.pavlig43.nocombro.MainKt"

        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            includeAllModules = true
            this.packageName = "ru.pavlig43.nocombro"
            this.packageVersion = libs.versions.versionName.get()
            windows {
                shortcut = true
                menu = true
                menuGroup = "Nocombro"
                iconFile.set(project.file("src/desktopMain/resources/icons/nocombro.ico"))
            }
        }
    }
}


