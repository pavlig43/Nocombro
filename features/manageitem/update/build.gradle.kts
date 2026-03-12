import ru.pavlig43.convention.extension.desktopDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)
  }


kotlin{
    desktopDependencies {
        implementation(projects.database)
        implementation(projects.features.table.immutable)
        implementation(projects.features.manageitem.loadinitdata)

    }
}