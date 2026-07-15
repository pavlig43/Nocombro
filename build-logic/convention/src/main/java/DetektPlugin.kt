import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.DetektGenerateConfigTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import ru.pavlig43.convention.extension.libs

/**
 * Подключает единые настройки Detekt ко всем Kotlin-модулям проекта.
 *
 * Обычные проверки не меняют исходники и не блокируют сборку. Отдельная задача
 * `detektAutoFix` запускает автоисправление явно. Сгенерированный код исключается,
 * а Android KMP classpath получает тип артефакта, нужный Detekt 1.x при AGP 9.
 */
class DetektPlugin : Plugin<Project> {
    /** Настраивает Detekt после подключения любого Kotlin base plugin. */
    override fun apply(target: Project) {

        target.plugins.withType(KotlinBasePlugin::class.java) {
            with(target) {

                apply(plugin = libs.plugins.detekt.get().pluginId)

                // Detekt 1.x сам не запрашивает тип артефакта AGP 9 для KMP Android,
                // поэтому без атрибута project dependencies разрешаются неоднозначно.
                configurations.matching { it.name == "androidCompileClasspath" }.configureEach {
                    attributes.attribute(
                        Attribute.of("artifactType", String::class.java),
                        "android-classes-jar",
                    )
                }

                extensions.configure<DetektExtension> {

                    config.setFrom(rootProject.files("default-detekt-config.yml"))
                    buildUponDefaultConfig = false
                    autoCorrect = false
                    parallel = true
                    ignoreFailures = true

                }
                with(tasks) {
                    register<Detekt>("detektAutoFix") {
                        group = "formatting"
                        description = "Runs Detekt auto-correction for this project."
                        setSource(
                            target.fileTree(target.projectDir) {
                                include("src/**/*.kt")
                                include("*.gradle.kts")
                                exclude("**/build/**")
                            }
                        )
                        config.setFrom(rootProject.files("default-detekt-config.yml"))
                        buildUponDefaultConfig = false
                        autoCorrect = true
                        parallel = true
                        ignoreFailures = true
                    }
                    withType<Detekt> {

                        exclude {
                            val path = it.file.invariantSeparatorsPath
                            path.contains("/build/") ||
                                path.contains("/generated/") ||
                                path.contains("/ksp/") ||
                                path.contains("/room/")
                        }

                        reports {
                            html.required.set(true)
                            md.required.set(true)

                        }
                    }
                    withType<Detekt>().configureEach {
                        jvmTarget = libs.versions.java.get()
                        ignoreFailures = true
                        if (name != "detektAutoFix") {
                            autoCorrect = false
                        }
                    }
                    withType<DetektGenerateConfigTask>().configureEach {
                        enabled = false
                    }
                    withType<DetektCreateBaselineTask>().configureEach {
                        enabled = false
                    }
                }

                dependencies {
                    detektPlugins(libs.detekt.formatting)

                }
                pluginManager.withPlugin(libs.plugins.pavlig43.compose.get().pluginId) {
                    dependencies {
                        detektPlugins(libs.detekt.compose)
                    }
                }
                pluginManager.withPlugin(libs.plugins.pavlig43.decompose.get().pluginId) {
                    dependencies {
                        detektPlugins(libs.detekt.decompose)
                    }
                }


            }
        }
    }
}
/** Добавляет правило в отдельный classpath расширений Detekt. */
private fun DependencyHandlerScope.detektPlugins(dependency: Provider<MinimalExternalModuleDependency>){
    add("detektPlugins",dependency)
}
