import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.form.product"
}
kotlin{
    commonMainDependencies {
        implementation(projects.database)
        implementation(projects.features.manageitem.addfile)
        implementation(projects.features.manageitem.upsert)
        implementation(projects.features.manageitem.loadinitdata)
        
        
        implementation(projects.features.declarationtabslot)
        implementation(projects.features.itemlist)
    }
}