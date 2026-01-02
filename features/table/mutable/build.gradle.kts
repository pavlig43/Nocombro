import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }


kotlin{
    commonMainDependencies {
        implementation(projects.database)
        implementation(projects.features.manageitem.loadinitdata)
        implementation(projects.features.manageitem.upsert)
        implementation(libs.wwind.table.core)
        implementation(libs.wwind.table.format)
        implementation(libs.collections.immutable)
        api(projects.features.table.core)

    }
}