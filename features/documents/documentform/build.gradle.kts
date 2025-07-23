import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.documents.documentform"
}
kotlin{
    commonMainDependencies {
        implementation(projects.features.documents.addfile)
        implementation(projects.features.manageitem.managebasevaluesitem)
        implementation(projects.features.manageitem.loadinitdata)
        implementation(projects.database)
        implementation(libs.filekit.dialogs)

    }
}