import ru.pavlig43.convention.extension.desktopDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)
    alias(libs.plugins.pavlig43.table)
    alias(libs.plugins.pavlig43.testing)
  }


kotlin{
    desktopDependencies {
        implementation(projects.database)
        implementation(libs.filekit.dialogs)


    }
}
