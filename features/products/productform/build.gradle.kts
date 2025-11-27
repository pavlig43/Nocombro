import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.products.productform"
}
kotlin{
    commonMainDependencies {
        implementation(projects.database)
        implementation(projects.features.manageitem.addfile)
        implementation(projects.features.manageitem.require)
        implementation(projects.features.manageitem.loadinitdata)
        implementation(projects.features.manageitem.upsertitem)
        implementation(projects.features.manageitem.form)
        implementation(projects.features.declaration.declarationtabslot)
        implementation(projects.features.itemlist)
    }
}