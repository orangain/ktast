plugins {
    kotlin("multiplatform") version "1.9.0" // Kotlin plugin is required for Dokka to work properly.
    id("com.palantir.git-version") version "3.0.0"
    id("org.jetbrains.dokka") version "1.8.20"
}

val gitVersion: groovy.lang.Closure<String> by extra
val kotlinVersion by extra { "1.8.21" }

kotlin {
    jvm()
}

allprojects {
    group = "com.github.orangain.ktast"
    version = gitVersion()

    repositories {
        mavenCentral()
    }
}
