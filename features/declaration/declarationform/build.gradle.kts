import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.declaration.declarationform"
}
kotlin{
    commonMainDependencies {
        implementation(projects.features.manageitem.addfile)
        implementation(projects.features.itemlist)
        implementation(projects.features.manageitem.require)
        implementation(projects.features.manageitem.loadinitdata)
        implementation(projects.features.manageitem.upsertitem)
        implementation(projects.features.manageitem.form)
        implementation(projects.database)

    }
}