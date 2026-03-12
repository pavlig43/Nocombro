import ru.pavlig43.convention.extension.desktopDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }


desktopDependencies {
    implementation(projects.database)
    implementation(projects.datetime)

}