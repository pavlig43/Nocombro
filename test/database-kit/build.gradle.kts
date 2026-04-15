import ru.pavlig43.convention.extension.desktopDependencies

plugins {
    alias(libs.plugins.pavlig43.kmplibrary)
    alias(libs.plugins.pavlig43.coroutines)
}

desktopDependencies {
    api(projects.database)
    api(projects.test.kit)
}
