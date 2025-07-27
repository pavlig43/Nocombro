import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.products.productlist"
}
kotlin{
    commonMainDependencies {
        implementation(projects.features.itemlist)
        implementation(projects.database)
    }
}