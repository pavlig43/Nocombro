import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.desktopDependencies

plugins {
    alias(libs.plugins.pavlig43.kmplibrary)
    alias(libs.plugins.pavlig43.coroutines)
}

commonMainDependencies {
    api(libs.kotest.framework)
    api(libs.kotest.assertions)
    api(libs.kotlinx.coroutines.test)
    api(libs.koin.test)
    api(libs.turbine)
}

desktopDependencies {
    implementation(libs.kotlinx.coroutines.swing)
}
