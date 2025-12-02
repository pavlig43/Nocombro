import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.transaction.transactionform"
}
kotlin{
    commonMainDependencies {
        implementation(projects.database)
        implementation(projects.features.itemlist)
        implementation(projects.features.manageitem.loadinitdata)
        
        implementation(projects.features.manageitem.upsert)
        
    }
}