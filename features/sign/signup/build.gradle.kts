import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.sign.signsignup"
}
kotlin{
    commonMainDependencies {
        implementation(projects.features.sign.common)
    }
}