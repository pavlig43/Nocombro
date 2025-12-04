import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
    alias(libs.plugins.pavlig43.feature)
  }

android {
    namespace = "ru.pavlig43.declarationtabslot"
}
dependencies {
    commonMainDependencies {
        implementation(projects.database)
        implementation(projects.features.manageitem.loadinitdata)
        implementation(projects.features.manageitem.upsert)
implementation(projects.features.form.declaration)
        implementation(projects.features.itemlist)

    }
}

