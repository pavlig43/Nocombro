import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }


kotlin{
    commonMainDependencies {
        implementation(projects.features.manageitem.addfile)
        implementation(projects.features.manageitem.upsert)
        implementation(projects.features.manageitem.loadinitdata)

        
        implementation(projects.features.table.immutable)
        implementation(projects.database)

    }
}