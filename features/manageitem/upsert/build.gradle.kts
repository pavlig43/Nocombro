import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }


kotlin{
    commonMainDependencies {
        implementation(projects.database)
        implementation(projects.features.itemlist)
        implementation(projects.features.manageitem.loadinitdata)
        
    }
}