import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.itemlist"
}
kotlin{
    commonMainDependencies {
        implementation(projects.database)
    }
}