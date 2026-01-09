import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }


kotlin{
    commonMainDependencies {
        implementation(projects.features.files)
        implementation(projects.features.manageitem.upsert)
        implementation(projects.features.manageitem.loadinitdata)


        implementation(projects.features.table.immutable)
        implementation(projects.database)

        implementation(libs.wwind.table.core)
        implementation(libs.wwind.table.format)
        implementation(libs.collections.immutable)
        implementation(projects.features.table.core)



    }
}