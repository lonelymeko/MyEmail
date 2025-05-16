rootProject.name = "MyEmail"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://mvnrepository.com")
//       maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        maven("https://maven.aliyun.com/repository/public")
        mavenCentral()
        google()
        gradlePluginPortal()
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

dependencyResolutionManagement {
    repositories {
//       maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        maven("https://mvnrepository.com")
        mavenCentral()
        google()
        gradlePluginPortal()
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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

include(":composeApp")