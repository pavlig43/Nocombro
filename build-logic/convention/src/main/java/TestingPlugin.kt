import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import ru.pavlig43.convention.extension.androidDebugDependencies
import ru.pavlig43.convention.extension.androidInstrumentedTestDependencies
import ru.pavlig43.convention.extension.commonTestDependencies
import ru.pavlig43.convention.extension.desktopTestDependencies
import ru.pavlig43.convention.extension.libs

/**
 * Convention plugin для настройки тестирования в Kotlin Multiplatform проектах.
 *
 * Добавляет зависимости для:
 * - Kotest (BDD testing framework)
 * - MockK (mocking framework)
 * - Turbine (Flow testing)
 * - Kotlinx Coroutines Test
 * - Room Testing
 * - Compose UI Testing
 *
 * Также настраивает Test tasks для работы с Kotest JUnit Runner.
 */
class TestingPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Common тестовые зависимости (для всех платформ)
            commonTestDependencies {
                implementation(libs.kotest.framework)
                implementation(libs.kotest.assertions)
                implementation(libs.mockk)
                implementation(libs.turbine)
                implementation(libs.kotlinx.coroutines.test)
            }

            // JVM специфичные тестовые зависимости
            desktopTestDependencies {
                implementation(libs.kotest.junit.runner)
            }

            // Android Instrumented тестовые зависимости
            androidInstrumentedTestDependencies {
                implementation(libs.room.testing)
                implementation(libs.compose.ui.test)
            }

            // Android debug dependencies для UI тестов с compose-ui-test-manifest
            androidDebugDependencies {
                implementation(libs.compose.ui.test.manifest)
            }

            // Настройка Test tasks для Kotest
            tasks.withType<Test> {
                // Kotest использует JUnit Platform
                useJUnitPlatform()

                // Показывать полное имя теста в логах
                testLogging {
                    showStandardStreams = true
                    events(
                        org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                        org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                        org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
                    )
                }
            }
        }
    }
}
