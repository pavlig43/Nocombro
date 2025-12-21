import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import ru.pavlig43.convention.extension.androidMainDependencies
import ru.pavlig43.convention.extension.configureAndroid
import ru.pavlig43.convention.extension.kotlinMultiplatformConfig
import ru.pavlig43.convention.extension.libs
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.androidApplication.get().pluginId)
            apply(plugin = libs.plugins.pavlig43.kmp.get().pluginId)
            apply(plugin = libs.plugins.pavlig43.compose.get().pluginId)

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                androidMainDependencies {
                    implementation(libs.androidx.activity.compose)
                }
                defaultConfig {

                    targetSdk = libs.versions.android.targetSdk.get().toInt()
                    versionCode = libs.versions.versionCode.get().toInt()
                    versionName = libs.versions.versionName.get().toString()
                    //            applicationVariants.all {
                    //                val variant = this
                    //                outputs.all {
                    //                    if (this is ApkVariantOutputImpl) {
                    //                        this.outputFileName =
                    //                            "retromeet_${variant.name}_${variant.versionCode}_${variant.versionName}_${getApkBuildTime()}.apk"
                    //                    }
                    //                }
                    //            }
                    //            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
                        isDebuggable = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                    release {
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
            kotlinMultiplatformConfig {
                targets
                    .filterIsInstance<KotlinNativeTarget>()
                    .forEach { nativeTarget ->
                        nativeTarget.binaries.framework {
                            baseName = "${this@with.name}App"
                            isStatic = true
                        }
                    }
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

