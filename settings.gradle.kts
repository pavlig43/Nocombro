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
include(
    "app:desktopApp"
)

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
    ":features:form:expense",
)

include(
    ":features:analytic:main",
    ":features:analytic:profitability",
)

include(
    ":features:manageitem:update",
    ":features:manageitem:loadinitdata",
)
include(
    ":features:sign:common",
    ":features:sign:signin",
    ":features:sign:signup",
    ":features:sign:root"
)

include(":features:notification")
include(":datetime")
include(":features:sampletable")
include(":features:storage")
include(":features:doctor")



