import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.product"
}
kotlin{
    commonMainDependencies {
        implementation(projects.database)
    }
}