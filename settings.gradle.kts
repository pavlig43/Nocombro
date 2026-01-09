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
    ":features:table:core",
    ":features:table:immutable",
    ":features:table:mutable",
)
include("features:files")


include(
    ":features:form:document",
    ":features:form:product",
    ":features:form:vendor",
    ":features:form:declaration",
    ":features:form:transaction",
)

include(
    ":features:manageitem:upsert",
    ":features:manageitem:loadinitdata",
)
include(
    ":features:sign:common",
    ":features:sign:signin",
    ":features:sign:signup",
    ":features:sign:root"
)

include(":features:notification")

