import androidx.room.gradle.RoomExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import ru.pavlig43.convention.extension.commonMainDependencies
import ru.pavlig43.convention.extension.libs

class RoomPlugin:Plugin<Project> {
    override fun apply(target: Project) {
        with(target){


            apply(plugin = libs.plugins.room.get().pluginId)
            apply(plugin = libs.plugins.ksp.get().pluginId)

            configureRoomKmp()
        }
    }
}
private fun Project.configureRoomKmp(){
    extensions.configure<KspExtension> {
        arg("room.generateKotlin", "true")
    }
    extensions.configure<RoomExtension> {
        // The schemas directory contains a schema file for each version of the Room database.
        // This is required to enable Room auto migrations.
        // See https://developer.android.com/reference/kotlin/androidx/room/AutoMigration.
        schemaDirectory("$projectDir/schemas")
    }

    dependencies {
        add("kspCommonMainMetadata", libs.androidx.room.compiler)
        add("kspAndroid", libs.androidx.room.compiler)
//            add("kspIosX64", libs.androidx.room.compiler)
//            add("kspIosArm64", libs.androidx.room.compiler)
//            add("kspIosSimulatorArm64", libs.androidx.room.compiler)
        add("kspDesktop", libs.androidx.room.compiler)

    }
    commonMainDependencies {

        api(libs.androidx.room.runtime)
        api(libs.androidx.sqlite.bundled)
    }

}