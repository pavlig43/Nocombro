import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }


commonMainDependencies {
    implementation(projects.database)
    
}