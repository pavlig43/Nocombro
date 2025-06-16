package ru.pavlig43.convention.extension

import org.gradle.api.Project

fun Project.configureSqlDelight(){
    kotlinMultiplatformConfig {

        commonMainDependencies {
            implementation(libs.sqldelight.coroutines)
        }
        androidMainDependencies {
            implementation(libs.sqldelight.android)
        }
        desktopDependencies {
            implementation(libs.sqldelight.desktop)
        }

    }

}