import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
    alias(libs.plugins.pavlig43.feature)
//    alias(libs.plugins.pavlig43.decompose)

}

android{
    namespace = "ru.pavlig43.rootnocombro"
}
kotlin{
    commonMainDependencies {
        implementation(projects.features.sign.root)
        implementation(projects.features.documents.documentform)
        implementation(projects.features.documents.documentlist)
        implementation(projects.features.products.productform)
        implementation(projects.database)
        implementation(projects.datastore)

    }
}
