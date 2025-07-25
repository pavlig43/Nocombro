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
include(":core")
include(":coreui")
include(":corekoin")
include(":database")
include(":datastore")
include(":theme")

include(":features:sign:common")
include(":features:sign:signin")
include(":features:sign:signup")
include(":features:sign:root")

include(":rootnocombro")





include(":features:product")

include(":features:itemlist")
include(":features:manageitem:managebasevaluesitem")
include(":features:manageitem:loadinitdata")



include(":features:documents:addfile")
include(":features:documents:documentform")
include(":features:documents:documentlist")

