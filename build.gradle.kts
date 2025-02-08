import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "2.1.10" // Kotlin plugin is required for Dokka to work properly.
    id("org.jetbrains.dokka") version "1.9.20"
}

val kotlinVersion by extra { "2.1.10" }

kotlin {
    jvm {
        tasks.withType<KotlinCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_1_8)
            }
        }
    }
}

allprojects {
    group = "io.github.orangain.ktast"
    version = "0.9.4"

    repositories {
        mavenCentral()
    }
}
