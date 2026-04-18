import ru.pavlig43.convention.extension.desktopDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)
    alias(libs.plugins.pavlig43.table)
  }


kotlin{
    desktopDependencies {
        implementation(projects.database)
        implementation(projects.features.table.core)
        api(projects.datetime)


    }
}
