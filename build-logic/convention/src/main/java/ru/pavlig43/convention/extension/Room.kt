package ru.pavlig43.convention.extension

import androidx.room.gradle.RoomExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureRoomKmp(){
    extensions.configure<KspExtension> {
        arg("room.generateKotlin", "true")
    }
    extensions.configure<RoomExtension> {
        // The schemas directory contains a schema file for each version of the Room database.
        // This is required to enable Room auto migrations.
        // See https://developer.android.com/reference/kotlin/androidx/room/AutoMigration.
        schemaDirectory("$projectDir/schemas")
    }

        dependencies {
            add("kspCommonMainMetadata", libs.androidx.room.compiler)
            add("kspAndroid", libs.androidx.room.compiler)
//            add("kspIosX64", libs.androidx.room.compiler)
//            add("kspIosArm64", libs.androidx.room.compiler)
//            add("kspIosSimulatorArm64", libs.androidx.room.compiler)
            add("kspDesktop", libs.androidx.room.compiler)

        }

    kotlinMultiplatformConfig {
        commonMainDependencies {

            api(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }

    }

}