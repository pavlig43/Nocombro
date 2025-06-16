package ru.pavlig43.convention.extension

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal fun Project.configureApplication() {
    configureAndroid()
    (androidExtension as BaseAppModuleExtension).apply {
        defaultConfig {

            targetSdk = libs.versions.android.targetSdk.get().toInt()
            versionCode = libs.versions.versionCode.get().toInt()
            versionName = libs.versions.versionName.get().toString()
            applicationVariants.all {
                val variant = this
                outputs.all {
                    if (this is ApkVariantOutputImpl) {
                        this.outputFileName =
                            "retromeet_${variant.name}_${variant.versionCode}_${variant.versionName}_${getApkBuildTime()}.apk"
                    }
                }
            }
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

            buildConfigField("String", "VERSION_NAME", "\"$versionName\"")
            buildConfigField("String", "BUILD_TIME", "\"${getBuildConfigTime()}\"")

//            buildConfigField(
//                "String",
//                "RETROMEET_API_BASE_URL",
//                "\" https://fbbd-149-40-52-113.ngrok-free.app\""
//            )
//            buildConfigField(
//                "String",
//                "RETROMEET_WS",
//                "\"wss://fbbd-149-40-52-113.ngrok-free.app\""
//            )
        }


        buildTypes {
            debug {
//                signingConfig = signingConfigs.getByName("release")
//                isMinifyEnabled = false
//                isShrinkResources = true
                isDebuggable = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
            release {
//                signingConfig = signingConfigs.getByName("release")
                isMinifyEnabled = true
                isShrinkResources = true
                isDebuggable = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )

            }
        }
        buildFeatures {
            buildConfig = true
        }
        packaging {
            dex {
                useLegacyPackaging = true
            }
            jniLibs {
                useLegacyPackaging = true
                excludes += setOf("META-INF/{AL2.0,LGPL2.1}")
            }
            resources {
                excludes += setOf(
                    "**/*.md",
                    "**/*.version",
                    "**/*.properties",
                    "**/**/*.properties",
                    "META-INF/{AL2.0,LGPL2.1}",
                    "META-INF/CHANGES",
                    "DebugProbesKt.bin",
                    "kotlin-tooling-metadata.json"
                )
            }
        }
    }


}

private fun getTime(pattern: String): String {
    val now = ZonedDateTime.now(ZoneId.of("Europe/Moscow"))
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return now.format(formatter)
}

private fun getApkBuildTime(): String {
    return getTime(pattern = "yyyy_MM_dd_HH_mm_ss")
}

private fun getBuildConfigTime(): String {
    return getTime(pattern = "yyyy-MM-dd HH:mm:ss")
}
fun Project.composeDesktopApplication(
    mainClass: String,
    packageName: String,
    version: String = libs.versions.versionName.get().toString(),
    targetFormats: List<TargetFormat> = listOf(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
) {
    extensions.getByType<ComposeExtension>().extensions.configure<DesktopExtension> {
        application {
            this.mainClass = mainClass

            nativeDistributions {
                targetFormats(*targetFormats.toTypedArray())
                this.packageName = packageName
                this.packageVersion = version
            }
        }
    }
}