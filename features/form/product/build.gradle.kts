import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)
    alias(libs.plugins.pavlig43.table)
  }


kotlin{
    commonMainDependencies {
        implementation(projects.database)
        implementation(projects.features.files)
        implementation(projects.features.manageitem.update)
        implementation(projects.features.manageitem.loadinitdata)
        
        
        
        implementation(projects.features.table.immutable)
        implementation(projects.features.table.mutable)
        implementation(projects.features.table.core)

    }
}