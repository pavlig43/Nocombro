import ru.pavlig43.convention.extension.commonMainDependencies

plugins {
  alias(libs.plugins.pavlig43.feature)  
  }

android {
    namespace = "ru.pavlig43.itemlist"
}
kotlin{
    commonMainDependencies {
        implementation(projects.database)
        implementation(projects.features.manageitem.loadinitdata)
        implementation(libs.kotlinx.datetime)
//        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")

        // Для Compose (если используешь viewModel())
        implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

        // Для KMP (если multiplatform)
        implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.6")
        implementation("com.seanproctor:data-table-material3:0.11.4")

        implementation("ua.wwind.table-kmp:table-core:1.7.3")

        implementation("ua.wwind.table-kmp:table-format:1.7.3")
        implementation("ua.wwind.paging:paging-core:2.2.3")
        implementation("ua.wwind.table-kmp:table-paging:1.7.3")

        implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")
    }
}