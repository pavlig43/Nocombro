import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.library)
  alias(libs.plugins.pavlig43.kmp)
  alias(libs.plugins.pavlig43.decompose)

  alias(libs.plugins.pavlig43.coroutines)
  alias(libs.plugins.pavlig43.serialization)

  }

android {
    namespace = "ru.pavlig43.core"
}
kotlin{
    commonMainDependencies {
        implementation(libs.decompose)
        implementation(libs.kotlinx.datetime)

    }
}