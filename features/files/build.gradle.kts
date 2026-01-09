import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }


kotlin{
    commonMainDependencies {
        implementation(libs.filekit.dialogs)
        implementation(libs.filekit.dialogs.compose)
        implementation(projects.database)
        implementation(projects.features.manageitem.loadinitdata)
        implementation(projects.features.manageitem.upsert)

    }
}