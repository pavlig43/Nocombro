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
        implementation(projects.features.manageitem.loadinitdata)
        implementation("com.seanproctor:data-table-material3:0.11.4")
    }
}