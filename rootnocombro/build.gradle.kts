import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
    alias(libs.plugins.pavlig43.feature)


}

android{
    namespace = "ru.pavlig43.rootnocombro"
}
kotlin{
    commonMainDependencies {
        implementation(projects.features.sign.root)
        implementation(projects.features.itemlist)
        implementation(projects.features.documents.documentform)
        implementation(projects.features.products.productform)
        implementation(projects.database)
        implementation(projects.datastore)

    }
}
