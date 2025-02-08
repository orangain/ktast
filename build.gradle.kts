import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
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

publishing {
    publications {
        register<MavenPublication>("maven") {
            pom {
                name.set(project.name)
                description.set("Ktast is a simple library to manipulate Kotlin source code as a set of AST objects.")
                url.set("https://github.com/orangain/ktast")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/orangain/ktast/blob/main/LICENSE")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("orangain")
                        name.set("Kota Kato")
                        email.set("orangain@gmail.com")
                    }
                }
                scm {
                    url.set("https://github.com/orangain/ktast")
                }
            }
        }
    }

    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}
