import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.manageitem.form"
}
kotlin{
    commonMainDependencies {
        implementation(projects.database)
        implementation(projects.features.manageitem.require)
        implementation(projects.features.manageitem.loadinitdata)
        implementation(projects.features.manageitem.upsertitem)
    }
}