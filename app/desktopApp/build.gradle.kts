import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
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

/**
 * Корень боевых данных приложения на локальной машине.
 *
 * По умолчанию берётся каталог `AppData/Roaming` текущего пользователя. Для запуска с другим
 * набором данных путь можно задать через `NOCOMBRO_PRODUCTION_APPDATA`. Значение должно указывать
 * на родительский каталог папки `Nocombro`, а не на саму папку `Nocombro`.
 *
 * Provider сохраняет вычисление ленивым и позволяет Gradle учитывать переменную среды при работе
 * configuration cache.
 */
val productionAppDataRoot = providers
    .environmentVariable("NOCOMBRO_PRODUCTION_APPDATA")
    .orElse(
        providers.systemProperty("user.home").map { userHome ->
            file(userHome).resolve("AppData/Roaming").absolutePath
        },
    )

/**
 * Подключает к задаче запуска боевые настройки Nocombro.
 *
 * Задача получает:
 * - `APPDATA`, чтобы загрузчик S3 прочитал `Nocombro/s3.properties` из боевого каталога;
 * - `NOCOMBRO_YDB_JDBC_URL` с адресом основной базы YDB;
 * - `NOCOMBRO_YDB_SA_FILE` с путём к JSON-ключу сервисного аккаунта YDB.
 *
 * Содержимое `s3.properties` и JSON-ключа не читается Gradle-скриптом и не попадает в логи.
 * Перед стартом JVM проверяется наличие обоих файлов. При отсутствии файла задача завершается
 * с ошибкой до запуска приложения, чтобы оно не перешло на тестовые или пустые настройки.
 */
fun JavaExec.useProductionEnvironment() {
    val appDataRoot = file(productionAppDataRoot.get())
    val appDataDirectory = appDataRoot.resolve("Nocombro")
    val s3PropertiesFile = appDataDirectory.resolve("s3.properties")
    val ydbServiceAccountFile = appDataDirectory.resolve("ydb-sa-key.json")

    environment("APPDATA", appDataRoot.absolutePath)
    environment(
        "NOCOMBRO_YDB_JDBC_URL",
        "jdbc:ydb:grpcs://ydb.serverless.yandexcloud.net:2135/" +
            "?database=/ru-central1/b1g87p6oufggn8merjua/etn8eb6ujifrk8lp7b73",
    )
    environment("NOCOMBRO_YDB_SA_FILE", ydbServiceAccountFile.absolutePath)

    doFirst {
        if (!s3PropertiesFile.isFile) {
            throw GradleException("Production S3 properties file not found: $s3PropertiesFile")
        }
        if (!ydbServiceAccountFile.isFile) {
            throw GradleException("Production YDB service account file not found: $ydbServiceAccountFile")
        }
    }
}

/**
 * Имена задач, которые пользователь передал Gradle как точки входа.
 *
 * Android Studio обычно передаёт полный путь вида `:app:desktopApp:runWithEnv`. Здесь путь
 * сводится к последнему сегменту, чтобы одинаково обрабатывать запуск из окна Gradle и консоли.
 */
val requestedTaskNames = gradle.startParameter.taskNames
    .map { taskPath -> taskPath.substringAfterLast(':') }
    .toSet()

/**
 * Запускает desktop-приложение с боевыми S3 и YDB.
 *
 * Обёртка оставляет стандартную задачу `run` без изменений при обычном запуске. Боевое окружение
 * добавляется в `run` только тогда, когда точкой входа выбрана эта задача.
 */
val runWithEnv = tasks.register("runWithEnv") {
    group = "custom"
    description = "Runs the desktop app with the primary production YDB and S3 configuration."
    dependsOn("run")
}

/**
 * Запускает desktop-приложение с боевыми S3 и YDB через Compose Hot Reload.
 *
 * Обёртка использует штатную задачу `hotRunDesktop`, поэтому сохраняет её слежение за исходниками
 * и перезагрузку Compose. Средой запуска служит JBR, встроенный в Android Studio.
 */
val hotRunWithEnv = tasks.register("hotRunWithEnv") {
    group = "custom"
    description = "Runs the desktop app with Hot Reload and the primary production YDB and S3 configuration."
    dependsOn("hotRunDesktop")
}

/**
 * Добавляет боевое окружение в базовую Java-задачу выбранной обёртки.
 *
 * Compose создаёт `run` и `hotRunDesktop` не сразу, поэтому применяется `configureEach`: настройка
 * сработает и для уже созданной задачи, и для задачи, которую плагин зарегистрирует позже.
 * Проверка [requestedTaskNames] не даёт боевым переменным среды попасть в стандартные запуски.
 *
 * @param wrapperTaskName имя задачи из группы `custom`, выбранной как точка входа;
 * @param targetTaskName имя базовой Java-задачи, которая запускает JVM приложения.
 */
fun configureProductionEnvironmentFor(wrapperTaskName: String, targetTaskName: String) {
    if (wrapperTaskName !in requestedTaskNames) return

    tasks.withType<JavaExec>().configureEach {
        if (name == targetTaskName) {
            useProductionEnvironment()
        }
    }
}

/**
 * Назначает `hotRunDesktop` встроенный JetBrains Runtime 25 из Android Studio.
 *
 * Compose Hot Reload по умолчанию запрашивает JBR 21. На этой машине проект собирается под Java 21,
 * но Android Studio 2026.1.3 поставляется с JBR 25. Новая среда умеет запускать байткод Java 21 и
 * поддерживает расширенную замену классов, которая нужна Hot Reload.
 *
 * Путь к JBR объявлен через `org.gradle.java.installations.paths` в `gradle.properties`. Здесь Gradle
 * выбирает из найденных сред JetBrains Runtime версии 25. Явное значение [JavaExec.javaLauncher]
 * заменяет JBR 21, заданный плагином по умолчанию, и не запускает загрузку новой среды.
 */
fun configureAndroidStudioRuntimeForHotReload() {
    if ("hotRunWithEnv" !in requestedTaskNames) return

    val javaToolchains = extensions.getByType<JavaToolchainService>()
    tasks.withType<JavaExec>().configureEach {
        if (name == "hotRunDesktop") {
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(25))
                    vendor.set(JvmVendorSpec.JETBRAINS)
                },
            )
        }
    }
}

configureProductionEnvironmentFor(wrapperTaskName = "runWithEnv", targetTaskName = "run")
configureProductionEnvironmentFor(wrapperTaskName = "hotRunWithEnv", targetTaskName = "hotRunDesktop")
configureAndroidStudioRuntimeForHotReload()


