import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "2.0.0" // Kotlin plugin is required for Dokka to work properly.
    id("com.palantir.git-version") version "3.0.0"
    id("org.jetbrains.dokka") version "1.9.20"
}

val gitVersion: groovy.lang.Closure<String> by extra
val kotlinVersion by extra { "1.9.22" }

kotlin {
    jvm {
        tasks.withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
}

allprojects {
    group = "com.github.orangain.ktast"
    version = gitVersion()

    repositories {
        mavenCentral()
    }
}
