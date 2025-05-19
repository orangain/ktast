import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "2.1.10" // Kotlin plugin is required for Dokka to work properly.
    id("com.palantir.git-version") version "3.3.0"
    id("org.jetbrains.dokka") version "2.0.0"
}

val gitVersion: groovy.lang.Closure<String> by extra
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
    group = "com.github.orangain.ktast"
    version = gitVersion()

    repositories {
        mavenCentral()
    }
}
