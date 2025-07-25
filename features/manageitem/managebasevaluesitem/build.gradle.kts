import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.manageitem.managebasevaluesitem"
}
kotlin{
    commonMainDependencies {
        implementation(projects.database)
        implementation(projects.features.manageitem.loadinitdata)
    }
}