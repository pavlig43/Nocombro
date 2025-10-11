rootProject.name = "Nocombro"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

@Suppress("UnstableApiUsage")
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")
include(":rootnocombro")
include(":core")
include(":coreui")
include(":corekoin")
include(":database")
include(":datastore")
include(":theme")




include(
    ":features:itemlist",
    )

include(
    ":features:manageitem:require",
    ":features:manageitem:form",
    ":features:manageitem:addfile",
    ":features:manageitem:loadinitdata",
    ":features:manageitem:upsertitem",
    ":features:manageitem:declaration"
    )

include(
    ":features:sign:common",
    ":features:sign:signin",
    ":features:sign:signup",
    ":features:sign:root"
)

include(
    ":features:documents:documentform",
)


include(
    ":features:products:productform",
    )
include(
    ":features:vendor",
)



include(":features:notification")