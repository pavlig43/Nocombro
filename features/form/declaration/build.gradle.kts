import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.form.declaration"
}
kotlin{
    commonMainDependencies {
        implementation(projects.features.manageitem.addfile)
        implementation(projects.features.itemlist)
        implementation(projects.features.manageitem.upsert)
        implementation(projects.features.manageitem.loadinitdata)
        
        
        implementation(projects.database)

    }
}