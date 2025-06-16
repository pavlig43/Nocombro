import org.gradle.kotlin.dsl.support.kotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}
group = "ru.pavlig43.buildlogic"
private val projectJavaVersion: JavaVersion = JavaVersion.toVersion(libs.versions.java.get())

java {
    sourceCompatibility = projectJavaVersion
    targetCompatibility = projectJavaVersion
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(projectJavaVersion.toString()))
    }
}
tasks.withType<JavaCompile> {
    sourceCompatibility = libs.versions.java.get()
    targetCompatibility = libs.versions.java.get()
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    // Workaround for version catalog working inside precompiled scripts
    // Issue - https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
gradlePlugin {
    plugins {

        register("kmp") {
            id = libs.plugins.pavlig43.kmp.get().pluginId
            implementationClass = "KmpPlugin"
        }
        register("application") {
            id = libs.plugins.pavlig43.application.get().pluginId
            implementationClass = "ApplicationPlugin"
        }
        register("library") {
            id = libs.plugins.pavlig43.library.get().pluginId
            implementationClass = "LibraryPlugin"
        }
        register("compose") {
            id = libs.plugins.pavlig43.compose.get().pluginId
            implementationClass = "ComposePlugin"
        }
        register("serialization") {
            id = libs.plugins.pavlig43.serialization.get().pluginId
            implementationClass = "SerializationPlugin"
        }
        register("room") {
            id = libs.plugins.pavlig43.room.get().pluginId
            implementationClass = "RoomPlugin"
        }
        register("sqldelight") {
            id = libs.plugins.pavlig43.sqldelight.get().pluginId
            implementationClass = "SqlDelightPlugin"
        }
        register("coroutines") {
            id = libs.plugins.pavlig43.coroutines.get().pluginId
            implementationClass = "CoroutinesPlugin"
        }
        register("koin") {
            id = libs.plugins.pavlig43.koin.get().pluginId
            implementationClass = "KoinPlugin"
        }
        register("ktor") {
            id = libs.plugins.pavlig43.ktor.get().pluginId
            implementationClass = "KtorPlugin"
        }
        register("detekt") {
            id = libs.plugins.pavlig43.detekt.get().pluginId
            implementationClass = "DetektPlugin"
        }
        register("decompose") {
            id = libs.plugins.pavlig43.decompose.get().pluginId
            implementationClass = "DecomposePlugin"
        }
        register("feature"){
            id = libs.plugins.pavlig43.feature.get().pluginId
            implementationClass = "FeaturePlugin"
        }
    }
}



