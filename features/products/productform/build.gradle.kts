import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.products.productform"
}
kotlin{
    commonMainDependencies {
        implementation(projects.features.manageitem.managebasevaluesitem)
        implementation(projects.features.manageitem.loadinitdata)
        implementation(projects.features.manageitem.upsertitem)
        implementation(projects.database)
    }
}