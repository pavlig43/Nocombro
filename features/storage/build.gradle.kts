import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
    alias(libs.plugins.pavlig43.feature)
    alias(libs.plugins.pavlig43.table)
}

kotlin{
    commonMainDependencies {
        implementation(projects.database)
        implementation(projects.features.table.core)
    }
}