import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }


kotlin{
    commonMainDependencies {
        implementation(projects.database)
//        implementation(projects.features.manageitem.loadinitdata)
        implementation(libs.wwind.table.core)
        implementation(libs.wwind.table.format)
        implementation(libs.collections.immutable)

    }
}