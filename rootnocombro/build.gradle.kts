import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
    alias(libs.plugins.pavlig43.feature)


}

android {
    namespace = "ru.pavlig43.rootnocombro"
}
kotlin {
    commonMainDependencies {
        implementation(projects.features.sign.root)
        implementation(projects.features.notification)
        implementation(projects.features.itemlist)

        implementation(projects.database)
        implementation(projects.datastore)
        implementation(projects.features.manageitem.upsert)

        implementation(projects.features.form.document)
        implementation(projects.features.form.product)
        implementation(projects.features.form.vendor)
        implementation(projects.features.form.declaration)
        implementation(projects.features.form.transaction)

        implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.6")

    }
}
