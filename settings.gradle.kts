pluginManagement {
    includeBuild("project-xross/xross-plugin")

    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "meteor-maven-snapshots"
            url = uri("https://maven.meteordev.org/snapshots")
        }
        maven {
            name = "babbaj-repo"
            url = uri("https://babbaj.github.io/maven/")
        }
        mavenCentral()
        gradlePluginPortal()
    }

    val loomVersion: String by settings
    plugins {
        id("fabric-loom") version loomVersion
        id("org.jetbrains.kotlin.jvm") version "2.3.10"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.3.10"
    }
}

// dependencyResolutionManagement を削除して build.gradle.kts のリポジトリ設定を有効にする

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "infinite"

includeBuild("project-xross")
