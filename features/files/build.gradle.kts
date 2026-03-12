import ru.pavlig43.convention.extension.desktopDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }


kotlin{
    desktopDependencies {
        implementation(libs.filekit.dialogs)
        implementation(libs.filekit.dialogs.compose)
        implementation(projects.database)
        implementation(projects.features.manageitem.loadinitdata)
        implementation(projects.features.manageitem.update)

    }
}