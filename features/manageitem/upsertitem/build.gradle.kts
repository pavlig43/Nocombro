import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
    alias(libs.plugins.pavlig43.feature)
  }

android {
    namespace = "ru.pavlig43.manageitem.upsertitem"
}
dependencies {
    commonMainDependencies {
        implementation(projects.database)


    }
}

